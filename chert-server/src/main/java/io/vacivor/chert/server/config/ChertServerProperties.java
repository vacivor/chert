package io.vacivor.chert.server.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chert.server")
public class ChertServerProperties {

  private Duration longPollingTimeout = Duration.ofSeconds(30);
  private Duration releaseMessageScanInterval = Duration.ofSeconds(1);

  public Duration getLongPollingTimeout() {
    return longPollingTimeout;
  }

  public void setLongPollingTimeout(Duration longPollingTimeout) {
    this.longPollingTimeout = longPollingTimeout;
  }

  public Duration getReleaseMessageScanInterval() {
    return releaseMessageScanInterval;
  }

  public void setReleaseMessageScanInterval(Duration releaseMessageScanInterval) {
    this.releaseMessageScanInterval = releaseMessageScanInterval;
  }
}
