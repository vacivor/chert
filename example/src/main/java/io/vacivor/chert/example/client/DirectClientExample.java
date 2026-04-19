package io.vacivor.chert.example.client;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertClientConfig;
import io.vacivor.chert.client.ChertClients;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

public final class DirectClientExample {

  private DirectClientExample() {
  }

  public static void main(String[] args) throws InterruptedException {
    ChertClient client = ChertClients.create(new ChertClientConfig(
        URI.create("http://localhost:8080"),
        Duration.ofSeconds(2),
        "example-app",
        "example-access-key",
        "dev",
        "application.yml",
        null,
        Duration.ofMinutes(5)));

    try {
      // 1. Fetch content
      var response = client.fetchConfig();
      System.out.println("Loaded application.yml content: " + response.content());

      // 2. Add listener for another config
      client.addListener("database.properties", content -> {
        System.out.println("Async callback! database.properties changed: " + content);
      });

      // 3. Fetch entries (KV)
      Map<String, String> entries = client.fetchEntries("feature-flags");
      System.out.println("Loaded feature-flags: " + entries);

      // 4. Update entries
      client.updateEntries("feature-flags", Map.of("new-feature", "true"));
      System.out.println("Updated feature-flags successfully");

      // Keep alive to see listener triggered if server notifies
      System.out.println("Waiting for 10 seconds to observe notifications...");
      Thread.sleep(10000);

    } catch (RuntimeException exception) {
      System.err.println("Direct client example failed. Start :chert-server:bootRun first.");
      exception.printStackTrace(System.err);
    } finally {
      client.close();
    }
  }

}
