package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.config.ChertServerProperties;
import io.vacivor.chert.server.domain.config.ReleaseMessage;
import io.vacivor.chert.server.infrastructure.persistence.config.ReleaseMessageRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class ReleaseMessageScanner implements InitializingBean, DisposableBean {

  private static final Logger logger = LoggerFactory.getLogger(ReleaseMessageScanner.class);
  private static final int MISSING_RELEASE_MESSAGE_MAX_AGE = 10;

  private final ReleaseMessageRepository releaseMessageRepository;
  private final ChertServerProperties properties;

  private final ThreadPoolTaskScheduler scheduler;

  private final List<ReleaseMessageListener> listeners = new CopyOnWriteArrayList<>();
  private final Map<Long, Integer> missingReleaseMessages = new ConcurrentHashMap<>();

  private long lastProcessedId = 0;

  public ReleaseMessageScanner(
      ReleaseMessageRepository releaseMessageRepository,
      ChertServerProperties properties) {
    this.releaseMessageRepository = releaseMessageRepository;
    this.properties = properties;

    this.scheduler = new ThreadPoolTaskScheduler();
    this.scheduler.setPoolSize(1);
    this.scheduler.setThreadNamePrefix("release-message-scanner-");
  }

  @Override
  public void afterPropertiesSet() {
    initialize();
    this.scheduler.initialize();
    this.scheduler.scheduleWithFixedDelay(this::scanMessages, properties.getReleaseMessageScanInterval());
    this.scheduler.scheduleWithFixedDelay(this::cleanOldMessages, java.time.Duration.ofHours(1));
    logger.info("ReleaseMessageScanner started with scan interval: {}", properties.getReleaseMessageScanInterval());
  }

  @Override
  public void destroy() {
    this.scheduler.shutdown();
    logger.info("ReleaseMessageScanner shutdown.");
  }

  public void addMessageListener(ReleaseMessageListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  void scanMessages() {
    try {
      scanMissingMessages();
      scanNewMessages();
    } catch (Exception e) {
      logger.error("Failed to scan release messages", e);
    }
  }

  private void scanNewMessages() {
    boolean hasMore = true;
    while (hasMore && !Thread.currentThread().isInterrupted()) {
      List<ReleaseMessage> messages = releaseMessageRepository.findFirst100ByIdGreaterThanOrderByIdAsc(lastProcessedId);
      if (messages.isEmpty()) {
        hasMore = false;
        continue;
      }

      fireMessageScanned(messages);
      
      int messageCount = messages.size();
      long newMaxId = messages.get(messageCount - 1).getId();
      
      // Check for gaps
      if (newMaxId - lastProcessedId > messageCount) {
        recordMissingIds(messages, lastProcessedId);
      }
      
      lastProcessedId = newMaxId;
      hasMore = messageCount == 100;
    }
  }

  private void scanMissingMessages() {
    if (missingReleaseMessages.isEmpty()) {
      return;
    }
    
    List<Long> missingIds = List.copyOf(missingReleaseMessages.keySet());
    Iterable<ReleaseMessage> messages = releaseMessageRepository.findAllById(missingIds);
    fireMessageScanned(messages);
    
    for (ReleaseMessage message : messages) {
      missingReleaseMessages.remove(message.getId());
    }
    
    growAndCleanMissingMessages();
  }

  private void growAndCleanMissingMessages() {
    missingReleaseMessages.entrySet().removeIf(entry -> {
      if (entry.getValue() > MISSING_RELEASE_MESSAGE_MAX_AGE) {
        return true;
      } else {
        entry.setValue(entry.getValue() + 1);
        return false;
      }
    });
  }

  private void recordMissingIds(List<ReleaseMessage> messages, long startId) {
    for (ReleaseMessage message : messages) {
      long currentId = message.getId();
      if (currentId - startId > 1) {
        for (long i = startId + 1; i < currentId; i++) {
          missingReleaseMessages.putIfAbsent(i, 1);
        }
      }
      startId = currentId;
    }
  }

  void cleanOldMessages() {
    try {
      ReleaseMessage latest = releaseMessageRepository.findFirstByOrderByIdDesc();
      if (latest != null && latest.getId() > 1000) {
        releaseMessageRepository.deleteByIdLessThan(latest.getId() - 1000);
        logger.info("Cleaned up old release messages, keep last 1000.");
      }
    } catch (Exception e) {
      logger.error("Failed to clean up old release messages", e);
    }
  }

  void initialize() {
    ReleaseMessage latest = releaseMessageRepository.findFirstByOrderByIdDesc();
    if (latest != null) {
      lastProcessedId = latest.getId();
    }
    logger.info("ReleaseMessageScanner initialized with lastProcessedId: {}", lastProcessedId);
  }

  private void fireMessageScanned(Iterable<ReleaseMessage> messages) {
    for (ReleaseMessage message : messages) {
      for (ReleaseMessageListener listener : listeners) {
        try {
          listener.handleMessage(message);
        } catch (Exception e) {
          logger.error("Failed to invoke message listener: " + listener.getClass().getName(), e);
        }
      }
    }
  }
}
