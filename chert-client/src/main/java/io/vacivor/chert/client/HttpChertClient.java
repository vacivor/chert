package io.vacivor.chert.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class HttpChertClient implements ChertClient {

  private static final String DEFAULT_CONFIG_NAME = "application";
  private final ChertClientConfig config;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Map<String, List<ChertConfigListener>> listenerMap = new ConcurrentHashMap<>();
  private final Map<String, Instant> lastUpdatedMap = new ConcurrentHashMap<>();
  private volatile long lastMessageId;
  private ScheduledExecutorService scheduler;
  private ExecutorService notificationExecutor;

  public HttpChertClient(ChertClientConfig config) {
    this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build(),
        new ObjectMapper().findAndRegisterModules(),
        Clock.systemUTC());
  }

  HttpChertClient(ChertClientConfig config, HttpClient httpClient, ObjectMapper objectMapper, Clock clock) {
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    startPolling();
    startNotificationWatch();
  }

  private void startNotificationWatch() {
    this.notificationExecutor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "chert-notification-watch-" + config.appId());
      thread.setDaemon(true);
      return thread;
    });
    this.notificationExecutor.submit(this::watchNotifications);
  }

  private void watchNotifications() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        Set<String> configNames = new HashSet<>(listenerMap.keySet());
        configNames.add(DEFAULT_CONFIG_NAME);
        configNames.addAll(config.configImports());

        String configNameParams = configNames.stream()
            .map(name -> "configName=" + URLEncoder.encode(name, StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        String query = String.format("appId=%s&env=%s&lastMessageId=%s&%s",
            URLEncoder.encode(config.appId(), StandardCharsets.UTF_8),
            URLEncoder.encode(config.env(), StandardCharsets.UTF_8),
            lastMessageId,
            configNameParams);

        URI uri = config.endpoint().resolve("/api/open/notifications?" + query);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(60)) // Wait up to 60s (server 30s)
            .header("X-Chert-Access-Key", config.accessKey())
            .header("X-Chert-Secret-Key", config.secretKey())
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          NotificationPayload payload = objectMapper.readValue(response.body(), NotificationPayload.class);
          if (payload.lastMessageId() != null) {
            lastMessageId = Math.max(lastMessageId, payload.lastMessageId());
          }
          if (payload.configNames() != null && !payload.configNames().isEmpty()) {
            for (String changedConfigName : payload.configNames()) {
              checkUpdate(changedConfigName);
            }
          }
        } else if (response.statusCode() == 304) {
          // No change, continue
        } else {
          // Error, backoff
          TimeUnit.SECONDS.sleep(5);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        System.err.println("Error watching notifications: " + e.getMessage());
        try {
          TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @Override
  public ChertConfigResponse fetchConfig() {
    return fetchConfig(DEFAULT_CONFIG_NAME);
  }

  @Override
  public ChertConfigResponse fetchConfig(String configName) {
    HttpRequest request = HttpRequest.newBuilder(buildRequestUri(configName))
        .timeout(config.requestTimeout())
        .header("X-Chert-Access-Key", config.accessKey())
        .header("X-Chert-Secret-Key", config.secretKey())
        .GET()
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        ServerPayload payload = objectMapper.readValue(response.body(), ServerPayload.class);
        lastUpdatedMap.put(configName, payload.updatedAt());
        if (Objects.equals(configName, DEFAULT_CONFIG_NAME)) {
          saveToCache(response.body());
        }
        return new ChertConfigResponse(payload.content(), payload.updatedAt(), payload.type(), payload.format());
      }
      if (Objects.equals(configName, DEFAULT_CONFIG_NAME)) {
        return loadFromCache().orElseThrow(() ->
            new IllegalStateException("Unexpected status from chert server: " + response.statusCode() + " " + response.body()));
      }
      throw new IllegalStateException("Unexpected status from chert server: " + response.statusCode() + " " + response.body());
    } catch (IOException exception) {
      if (Objects.equals(configName, DEFAULT_CONFIG_NAME)) {
        return loadFromCache().orElseThrow(() ->
            new UncheckedIOException("Failed to reach chert server and no local cache available", exception));
      }
      throw new UncheckedIOException(exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while calling chert server", exception);
    }
  }

  private void saveToCache(String json) {
    Path path = config.localCachePath();
    if (path == null) {
      return;
    }
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      Files.writeString(path, json);
    } catch (IOException e) {
      System.err.println("Failed to save config to local cache: " + e.getMessage());
    }
  }

  private Optional<ChertConfigResponse> loadFromCache() {
    Path path = config.localCachePath();
    if (path == null || !Files.exists(path)) {
      return Optional.empty();
    }
    try {
      String json = Files.readString(path);
      ServerPayload payload = objectMapper.readValue(json, ServerPayload.class);
      if (lastUpdatedMap.get(DEFAULT_CONFIG_NAME) == null) {
        lastUpdatedMap.put(DEFAULT_CONFIG_NAME, payload.updatedAt());
      }
      return Optional.of(new ChertConfigResponse(payload.content(), payload.updatedAt(), payload.type(), payload.format()));
    } catch (IOException e) {
      System.err.println("Failed to load config from local cache: " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Map<String, String> fetchEntries() {
    return fetchEntries(DEFAULT_CONFIG_NAME);
  }

  @Override
  public Map<String, String> fetchEntries(String configName) {
    ChertConfigResponse response = fetchConfig(configName);
    if (response.type() != ConfigType.ENTRIES) {
      throw new IllegalStateException("Config resource is not of type ENTRIES: " + configName);
    }
    try {
      List<Map<String, Object>> list = objectMapper.readValue(response.content(), new TypeReference<>() {});
      Map<String, String> result = new HashMap<>();
      for (Map<String, Object> entry : list) {
        Object key = entry.get("key");
        Object value = entry.get("value");
        if (key != null && value != null) {
          result.put(key.toString(), value.toString());
        }
      }
      return result;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to parse ENTRIES config", e);
    }
  }

  @Override
  public void addListener(ChertConfigListener listener) {
    addListener(DEFAULT_CONFIG_NAME, listener);
  }

  @Override
  public void addListener(String configName, ChertConfigListener listener) {
    Objects.requireNonNull(configName, "configName must not be null");
    Objects.requireNonNull(listener, "listener must not be null");
    this.listenerMap.computeIfAbsent(configName, k -> new CopyOnWriteArrayList<>()).add(listener);
  }

  @Override
  public void updateEntries(Map<String, String> entries) {
    updateEntries(DEFAULT_CONFIG_NAME, entries);
  }

  @Override
  public void updateEntries(String configName, Map<String, String> entries) {
    Objects.requireNonNull(configName, "configName must not be null");
    Objects.requireNonNull(entries, "entries must not be null");
    try {
      String json = objectMapper.writeValueAsString(entries);
      HttpRequest request = HttpRequest.newBuilder(buildRequestUri(configName))
          .timeout(config.requestTimeout())
          .header("X-Chert-Access-Key", config.accessKey())
          .header("X-Chert-Secret-Key", config.secretKey())
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException("Failed to update config entries: " + response.statusCode() + " " + response.body());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    if (this.scheduler != null) {
      this.scheduler.shutdown();
    }
    if (this.notificationExecutor != null) {
      this.notificationExecutor.shutdownNow();
    }
    try {
      if (this.scheduler != null && !this.scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
        this.scheduler.shutdownNow();
      }
      if (this.notificationExecutor != null && !this.notificationExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
        this.notificationExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      if (this.scheduler != null) this.scheduler.shutdownNow();
      if (this.notificationExecutor != null) this.notificationExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void startPolling() {
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread thread = new Thread(r, "chert-polling-" + config.appId());
      thread.setDaemon(true);
      return thread;
    });

    long delay = config.refreshInterval().toSeconds();
    this.scheduler.scheduleWithFixedDelay(this::checkUpdate, delay, delay, TimeUnit.SECONDS);
  }

  private void checkUpdate() {
    Set<String> configNames = new HashSet<>(listenerMap.keySet());
    configNames.add(DEFAULT_CONFIG_NAME);
    configNames.addAll(config.configImports());
    for (String configName : configNames) {
      checkUpdate(configName);
    }
  }

  void checkUpdate(String configName) {
    try {
      Instant previousLastUpdate = lastUpdatedMap.get(configName);
      ChertConfigResponse response = fetchConfig(configName);
      if (response.updatedAt() != null && (previousLastUpdate == null || response.updatedAt().isAfter(previousLastUpdate))) {
        List<ChertConfigListener> listeners = listenerMap.get(configName);
        if (listeners != null) {
          for (ChertConfigListener listener : listeners) {
            notifyListener(listener, response.content());
          }
        }
      }
    } catch (Exception e) {
      // Log error (simple print for now)
      System.err.println("Failed to poll config [" + configName + "] from Chert: " + e.getMessage());
    }
  }

  private void notifyListener(ChertConfigListener listener, String content) {
    Executor executor = listener.getExecutor();
    if (executor != null) {
      executor.execute(() -> {
        try {
          listener.onChange(content);
        } catch (Exception e) {
          System.err.println("Error in async listener: " + e.getMessage());
        }
      });
    } else {
      try {
        listener.onChange(content);
      } catch (Exception e) {
        System.err.println("Error in listener: " + e.getMessage());
      }
    }
  }

  private URI buildRequestUri(String configName) {
    String path = String.format("/api/open/configs/%s/%s/%s",
        URLEncoder.encode(config.appId(), StandardCharsets.UTF_8),
        URLEncoder.encode(config.env(), StandardCharsets.UTF_8),
        URLEncoder.encode(configName, StandardCharsets.UTF_8));
    return config.endpoint().resolve(path);
  }

  private record ServerPayload(String content, Instant updatedAt, ConfigType type, ConfigFormat format) {
  }

  private record NotificationPayload(Long lastMessageId, List<String> configNames) {
  }

}
