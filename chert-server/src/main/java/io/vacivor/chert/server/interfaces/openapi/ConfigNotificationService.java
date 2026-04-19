package io.vacivor.chert.server.interfaces.openapi;

import io.vacivor.chert.server.application.config.ReleaseMessageListener;
import io.vacivor.chert.server.application.config.ReleaseMessageScanner;
import io.vacivor.chert.server.domain.config.ReleaseMessage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

@Service
public class ConfigNotificationService implements ReleaseMessageListener {

  private record NotificationKey(String appId, String envCode, String configName) {}

  private final ConcurrentHashMap<NotificationKey, ConcurrentLinkedQueue<DeferredResult<ResponseEntity<String>>>> watchers = new ConcurrentHashMap<>();

  public ConfigNotificationService(ReleaseMessageScanner releaseMessageScanner) {
    releaseMessageScanner.addMessageListener(this);
  }

  @Override
  public void handleMessage(ReleaseMessage message) {
    notifyWatchers(message.getAppId(), message.getEnvCode(), message.getName());
  }

  public void notifyWatchers(String appId, String envCode, String configName) {
    NotificationKey key = new NotificationKey(appId, envCode, configName);
    ConcurrentLinkedQueue<DeferredResult<ResponseEntity<String>>> queue = watchers.get(key);
    if (queue != null) {
      DeferredResult<ResponseEntity<String>> result;
      while ((result = queue.poll()) != null) {
        if (!result.isSetOrExpired()) {
          result.setResult(ResponseEntity.ok(configName));
        }
      }
    }
  }

  public DeferredResult<ResponseEntity<String>> watch(String appId, String envCode, List<String> configNames) {
    DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>(30000L); // 30s timeout

    for (String configName : configNames) {
      NotificationKey key = new NotificationKey(appId, envCode, configName);
      watchers.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(deferredResult);
    }

    deferredResult.onCompletion(() -> {
      for (String configName : configNames) {
        NotificationKey key = new NotificationKey(appId, envCode, configName);
        ConcurrentLinkedQueue<DeferredResult<ResponseEntity<String>>> queue = watchers.get(key);
        if (queue != null) {
          queue.remove(deferredResult);
        }
      }
    });
    deferredResult.onTimeout(() -> deferredResult.setResult(ResponseEntity.status(HttpStatus.NOT_MODIFIED).build()));

    return deferredResult;
  }
}
