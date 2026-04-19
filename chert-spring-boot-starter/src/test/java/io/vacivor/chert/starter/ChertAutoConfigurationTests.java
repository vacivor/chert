package io.vacivor.chert.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfigListener;
import io.vacivor.chert.client.ChertConfigResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ChertAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ChertAutoConfiguration.class));

  @Test
  void shouldRegisterChertClientBean() {
    contextRunner
        .withPropertyValues(
            "chert.client.endpoint=http://localhost:8080",
            "chert.client.request-timeout=3s")
        .run(context -> assertThat(context).hasSingleBean(ChertClient.class));
  }

  @Test
  void shouldBackOffWhenUserProvidesOwnClient() {
    contextRunner
        .withBean(ChertClient.class, () -> new ChertClient() {
          @Override
          public ChertConfigResponse fetchConfig() { return null; }

          @Override
          public ChertConfigResponse fetchConfig(String configId) { return null; }

          @Override
          public Map<String, String> fetchEntries() { return null; }

          @Override
          public Map<String, String> fetchEntries(String configId) { return null; }

          @Override
          public void addListener(ChertConfigListener listener) {}

          @Override
          public void addListener(String configId, ChertConfigListener listener) {}

          @Override
          public void updateEntries(Map<String, String> entries) {}

          @Override
          public void updateEntries(String configId, Map<String, String> entries) {}
        })
        .run(context -> assertThat(context).hasSingleBean(ChertClient.class));
  }

}
