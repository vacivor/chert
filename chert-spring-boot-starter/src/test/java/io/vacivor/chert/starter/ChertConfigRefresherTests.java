package io.vacivor.chert.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfigResponse;
import io.vacivor.chert.client.ConfigFormat;
import io.vacivor.chert.client.ConfigType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChertConfigRefresherTests {

  @Test
  void refreshShouldUpdateEnvironmentAndRebindBean() throws InterruptedException {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestConfiguration.class);
    
    ChertClient chertClient = mock(ChertClient.class);
    ConfigurableEnvironment environment = new StandardEnvironment();
    
    // Set initial property
    environment.getPropertySources().addFirst(new MapPropertySource("chert:app.yml", 
        Collections.singletonMap("test.value", "old")));
    
    context.setEnvironment(environment);
    context.registerBean(ChertClient.class, () -> chertClient);
    context.refresh();

    TestProperties properties = context.getBean(TestProperties.class);
    assertThat(properties.getValue()).isEqualTo("old");

    // Prepare mock update
    String newContent = "test.value: new";
    ChertConfigResponse response = new ChertConfigResponse(newContent, Instant.now(), ConfigType.CONTENT, ConfigFormat.YAML);
    when(chertClient.fetchConfig("app.yml")).thenReturn(response);

    ChertConfigRefresher refresher = new ChertConfigRefresher(chertClient, environment);
    refresher.setApplicationContext(context);
    
    // Call refresh
    refresher.refresh("app.yml");
    Thread.sleep(200);

    // Verify
    assertThat(environment.getProperty("test.value")).isEqualTo("new");
    assertThat(properties.getValue()).isEqualTo("new");
    
    context.close();
  }

  @Test
  void refreshEntriesShouldNotUpdateEnvironment() throws InterruptedException {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestConfiguration.class);

    ChertClient chertClient = mock(ChertClient.class);
    ConfigurableEnvironment environment = new StandardEnvironment();

    // Set initial property
    environment.getPropertySources().addFirst(new MapPropertySource("chert:app-entries",
        Collections.singletonMap("test.value", "old")));

    context.setEnvironment(environment);
    context.registerBean(ChertClient.class, () -> chertClient);
    context.refresh();

    TestProperties properties = context.getBean(TestProperties.class);
    assertThat(properties.getValue()).isEqualTo("old");

    // Prepare mock update
    Map<String, String> newEntries = Collections.singletonMap("test.value", "new-entry");
    ChertConfigResponse response = new ChertConfigResponse("[]", Instant.now(), ConfigType.ENTRIES, ConfigFormat.JSON);
    when(chertClient.fetchConfig("app-entries")).thenReturn(response);

    ChertConfigRefresher refresher = new ChertConfigRefresher(chertClient, environment);
    refresher.setApplicationContext(context);

    // Call refresh
    refresher.refresh("app-entries");
    Thread.sleep(200);

    // Verify: should still be "old" because ENTRIES are ignored by refresher for Spring
    assertThat(environment.getProperty("test.value")).isEqualTo("old");
    assertThat(properties.getValue()).isEqualTo("old");

    context.close();
  }

  @Test
  void refreshShouldUpdateValueAnnotatedFields() throws InterruptedException {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    ChertClient chertClient = mock(ChertClient.class);
    ConfigurableEnvironment environment = new StandardEnvironment();

    // Set initial property
    environment.getPropertySources().addFirst(new MapPropertySource("chert:app.yml",
        Collections.singletonMap("test.value", "old")));

    context.setEnvironment(environment);
    context.registerBean(ChertClient.class, () -> chertClient);
    context.registerBean(ChertConfigRefresher.class, chertClient, environment);
    context.registerBean(TestValueBean.class);

    context.refresh();

    TestValueBean valueBean = context.getBean(TestValueBean.class);
    assertThat(valueBean.getValue()).isEqualTo("old");

    // Prepare mock update
    String newContent = "test.value: new-value";
    ChertConfigResponse response = new ChertConfigResponse(newContent, Instant.now(), ConfigType.CONTENT, ConfigFormat.YAML);
    when(chertClient.fetchConfig("app.yml")).thenReturn(response);

    ChertConfigRefresher refresher = context.getBean(ChertConfigRefresher.class);

    // Call refresh
    refresher.refresh("app.yml");
    Thread.sleep(200);

    // Verify
    assertThat(valueBean.getValue()).isEqualTo("new-value");

    context.close();
  }

  @Test
  void refreshShouldUpdateValueAnnotatedMethods() throws InterruptedException {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    ChertClient chertClient = mock(ChertClient.class);
    ConfigurableEnvironment environment = new StandardEnvironment();

    // Set initial property
    environment.getPropertySources().addFirst(new MapPropertySource("chert:app.yml",
        Collections.singletonMap("test.value", "old")));

    context.setEnvironment(environment);
    context.registerBean(ChertClient.class, () -> chertClient);
    context.registerBean(ChertConfigRefresher.class, chertClient, environment);
    context.registerBean(TestValueMethodBean.class);

    context.refresh();

    TestValueMethodBean valueBean = context.getBean(TestValueMethodBean.class);
    assertThat(valueBean.getValue()).isEqualTo("old");

    // Prepare mock update
    String newContent = "test.value: new-value";
    ChertConfigResponse response = new ChertConfigResponse(newContent, Instant.now(), ConfigType.CONTENT, ConfigFormat.YAML);
    when(chertClient.fetchConfig("app.yml")).thenReturn(response);

    ChertConfigRefresher refresher = context.getBean(ChertConfigRefresher.class);

    // Call refresh
    refresher.refresh("app.yml");
    Thread.sleep(200);

    // Verify
    assertThat(valueBean.getValue()).isEqualTo("new-value");

    context.close();
  }

  @Test
  void refreshShouldOnlyUpdateAffectedBeans() throws InterruptedException {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestConfiguration.class);
    context.registerBean(OtherProperties.class);

    ChertClient chertClient = mock(ChertClient.class);
    ConfigurableEnvironment environment = new StandardEnvironment();

    // Set initial properties
    Map<String, Object> initialMap = new java.util.HashMap<>();
    initialMap.put("test.value", "old");
    initialMap.put("other.value", "initial");
    environment.getPropertySources().addFirst(new MapPropertySource("chert:app.yml", initialMap));

    context.setEnvironment(environment);
    context.registerBean(ChertClient.class, () -> chertClient);
    context.refresh();

    TestProperties testProperties = context.getBean(TestProperties.class);
    OtherProperties otherProperties = context.getBean(OtherProperties.class);
    
    assertThat(testProperties.getValue()).isEqualTo("old");
    assertThat(otherProperties.getValue()).isEqualTo("initial");
    
    // reset count after initialization
    otherProperties.resetCount();

    // Prepare mock update: only test.value changes
    String newContent = "test.value: new\nother.value: initial";
    ChertConfigResponse response = new ChertConfigResponse(newContent, Instant.now(), ConfigType.CONTENT, ConfigFormat.YAML);
    when(chertClient.fetchConfig("app.yml")).thenReturn(response);

    ChertConfigRefresher refresher = new ChertConfigRefresher(chertClient, environment);
    refresher.setApplicationContext(context);
    // Trigger manual post-processing since we newed it
    refresher.postProcessBeforeInitialization(testProperties, "testProperties");
    refresher.postProcessBeforeInitialization(otherProperties, "otherProperties");

    // Call refresh
    refresher.refresh("app.yml");
    Thread.sleep(200);

    // Verify
    assertThat(testProperties.getValue()).isEqualTo("new");
    assertThat(otherProperties.getValue()).isEqualTo("initial");
    assertThat(otherProperties.getCount()).isZero(); // Should NOT have been rebound
    
    context.close();
  }

  @Configuration
  @EnableConfigurationProperties(TestProperties.class)
  static class TestConfiguration {
  }

  @ConfigurationProperties(prefix = "test")
  public static class TestProperties {
    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @ConfigurationProperties(prefix = "other")
  public static class OtherProperties {
    private String value;
    private int count = 0;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
      this.count++;
    }

    public int getCount() {
      return count;
    }

    public void resetCount() {
      this.count = 0;
    }
  }

  @Component
  public static class TestValueBean {
    @Value("${test.value}")
    private String value;

    public String getValue() {
      return value;
    }
  }

  @Component
  public static class TestValueMethodBean {
    private String value;

    @Value("${test.value}")
    public void setValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
