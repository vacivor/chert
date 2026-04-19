package io.vacivor.chert.starter;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chert.client")
public class ChertClientProperties {

  private String endpoint = "http://localhost:8080";
  private Duration requestTimeout = Duration.ofSeconds(2);
  private String appId;
  private String accessKey;
  private String secretKey;
  private String env = "default";
  private String localCachePath;
  private Duration refreshInterval = Duration.ofMinutes(5);

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }


  public String getLocalCachePath() {
    return localCachePath;
  }

  public void setLocalCachePath(String localCachePath) {
    this.localCachePath = localCachePath;
  }

  public Duration getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(Duration refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

}
