package io.vacivor.chert.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpChertClientTests {

  @Test
  void shouldNotifyListenerWithExecutor() throws Exception {
    ChertClientConfig config = new ChertClientConfig(
        URI.create("http://localhost:8080/"),
        Duration.ofSeconds(3),
        "test-app",
        "secret",
        "secret-key",
        "dev",
        null,
        Duration.ofMinutes(5)
    );

    HttpClient httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    // JSON matching ServerPayload record
    when(response.body()).thenReturn("{\"content\":\"new-val\",\"updatedAt\":\"" + Instant.now() + "\",\"type\":\"CONTENT\",\"format\":\"YAML\"}");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    Clock clock = Clock.systemUTC();

    HttpChertClient client = new HttpChertClient(config, httpClient, objectMapper, clock);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean invoked = new AtomicBoolean(false);
    Executor executor = command -> {
      new Thread(() -> {
        command.run();
        latch.countDown();
      }).start();
    };

    client.addListener(new AbstractChertConfigListener() {
      @Override
      public void onChange(String content) {
        if ("new-val".equals(content)) {
          invoked.set(true);
        }
      }

      @Override
      public Executor getExecutor() {
        return executor;
      }
    });

    client.checkUpdate("application");

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(invoked.get()).isTrue();
    client.close();
  }

  @Test
  void shouldIncludeMultipleConfigNamesInWatchRequest() throws Exception {
    ChertClientConfig config = new ChertClientConfig(
        URI.create("http://localhost:8080/"),
        Duration.ofSeconds(3),
        "test-app",
        "secret",
        "secret-key",
        "dev",
        null,
        Duration.ofMinutes(5)
    );

    HttpClient httpClient = mock(HttpClient.class);
    AtomicReference<URI> capturedUri = new AtomicReference<>();
    
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
      HttpRequest req = invocation.getArgument(0);
      URI uri = req.uri();
      if (uri.getPath().contains("/api/open/notifications")) {
        capturedUri.set(uri);
      }
      HttpResponse<String> response = mock(HttpResponse.class);
      when(response.statusCode()).thenReturn(304);
      return response;
    });

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    HttpChertClient client = new HttpChertClient(config, httpClient, objectMapper, Clock.systemUTC());

    client.addListener("other.yml", content -> {});

    // wait for watchNotifications to run and capture a URI that contains both configNames
    long start = System.currentTimeMillis();
    while (capturedUri.get() == null || !capturedUri.get().getQuery().contains("configName=other.yml")) {
      if (System.currentTimeMillis() - start > 5000) break;
      Thread.sleep(100);
    }

    URI uri = capturedUri.get();
    assertThat(uri).isNotNull();
    String query = uri.getQuery();
    assertThat(query).contains("appId=test-app");
    assertThat(query).contains("env=dev");
    assertThat(query).contains("configName=application");
    assertThat(query).contains("configName=other.yml");

    client.close();
  }

  @Test
  void shouldTriggerCheckUpdateWhenNotificationReceived() throws Exception {
    ChertClientConfig config = new ChertClientConfig(
        URI.create("http://localhost:8080/"),
        Duration.ofSeconds(3),
        "test-app",
        "secret",
        "secret-key",
        "dev",
        null,
        Duration.ofMinutes(5)
    );

    HttpClient httpClient = mock(HttpClient.class);

    HttpResponse<String> pollResponse = mock(HttpResponse.class);
    when(pollResponse.statusCode()).thenReturn(200);
    when(pollResponse.body()).thenReturn("application");

    HttpResponse<String> fetchResponse = mock(HttpResponse.class);
    when(fetchResponse.statusCode()).thenReturn(200);
    when(fetchResponse.body()).thenReturn("{\"content\":\"new-val\",\"updatedAt\":\"" + Instant.now() + "\",\"type\":\"CONTENT\",\"format\":\"YAML\"}");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenAnswer(invocation -> {
          HttpRequest req = invocation.getArgument(0);
          if (req.uri().getPath().contains("/api/open/notifications")) {
            return pollResponse;
          }
          return fetchResponse;
        });

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    HttpChertClient client = new HttpChertClient(config, httpClient, objectMapper, Clock.systemUTC());

    CountDownLatch latch = new CountDownLatch(1);
    client.addListener(content -> {
      if ("new-val".equals(content)) {
        latch.countDown();
      }
    });

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    client.close();
  }
}
