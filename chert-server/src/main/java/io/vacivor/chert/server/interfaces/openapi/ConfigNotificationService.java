package io.vacivor.chert.server.interfaces.openapi;

import io.vacivor.chert.server.config.ChertServerProperties;
import io.vacivor.chert.server.domain.config.ReleaseMessage;
import io.vacivor.chert.server.infrastructure.persistence.config.ReleaseMessageRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

@Service
public class ConfigNotificationService implements DisposableBean {

  private final ReleaseMessageRepository releaseMessageRepository;
  private final ChertServerProperties properties;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, runnable -> {
    Thread thread = new Thread(runnable, "config-notification-watch");
    thread.setDaemon(true);
    return thread;
  });

  public ConfigNotificationService(
      ReleaseMessageRepository releaseMessageRepository,
      ChertServerProperties properties) {
    this.releaseMessageRepository = releaseMessageRepository;
    this.properties = properties;
  }

  public DeferredResult<ResponseEntity<ConfigNotificationResponse>> watch(
      String appId,
      String envCode,
      List<String> configNames,
      Long lastMessageId) {
    long cursor = lastMessageId != null ? Math.max(0L, lastMessageId) : 0L;
    DeferredResult<ResponseEntity<ConfigNotificationResponse>> deferredResult =
        new DeferredResult<>(properties.getLongPollingTimeout().toMillis());

    ResponseEntity<ConfigNotificationResponse> immediateResponse =
        scanNotifications(appId, envCode, configNames, cursor);
    if (!immediateResponse.getBody().configNames().isEmpty()) {
      deferredResult.setResult(immediateResponse);
      return deferredResult;
    }

    AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
    Runnable pollTask = () -> {
      if (deferredResult.isSetOrExpired()) {
        ScheduledFuture<?> future = futureRef.get();
        if (future != null) {
          future.cancel(false);
        }
        return;
      }

      ResponseEntity<ConfigNotificationResponse> response =
          scanNotifications(appId, envCode, configNames, cursor);
      if (!response.getBody().configNames().isEmpty()) {
        deferredResult.setResult(response);
      }
    };

    ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
        pollTask,
        properties.getReleaseMessageScanInterval().toMillis(),
        properties.getReleaseMessageScanInterval().toMillis(),
        TimeUnit.MILLISECONDS);
    futureRef.set(future);

    deferredResult.onCompletion(() -> {
      ScheduledFuture<?> scheduledFuture = futureRef.get();
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
    });
    deferredResult.onTimeout(() -> deferredResult.setResult(
        ResponseEntity.ok(new ConfigNotificationResponse(cursor, List.of()))));

    return deferredResult;
  }

  private ResponseEntity<ConfigNotificationResponse> scanNotifications(
      String appId,
      String envCode,
      List<String> configNames,
      long lastMessageId) {
    if (configNames == null || configNames.isEmpty()) {
      return ResponseEntity.ok(new ConfigNotificationResponse(lastMessageId, List.of()));
    }

    List<ReleaseMessage> messages =
        releaseMessageRepository.findTop100ByAppIdAndEnvCodeAndNameInAndIdGreaterThanOrderByIdAsc(
            appId, envCode, configNames, lastMessageId);
    if (messages.isEmpty()) {
      return ResponseEntity.ok(new ConfigNotificationResponse(lastMessageId, List.of()));
    }

    Set<String> changedConfigNames = new LinkedHashSet<>();
    long latestMessageId = lastMessageId;
    for (ReleaseMessage message : messages) {
      changedConfigNames.add(message.getName());
      latestMessageId = Math.max(latestMessageId, message.getId());
    }

    return ResponseEntity.ok(new ConfigNotificationResponse(latestMessageId, List.copyOf(changedConfigNames)));
  }

  @Override
  public void destroy() {
    scheduler.shutdownNow();
  }
}
