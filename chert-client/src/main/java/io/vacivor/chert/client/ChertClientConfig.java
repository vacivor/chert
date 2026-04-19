package io.vacivor.chert.client;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record ChertClientConfig(
    URI endpoint,
    Duration requestTimeout,
    String appId,
    String accessKey,
    String secretKey,
    String env,
    Path localCachePath,
    Duration refreshInterval,
    Set<String> configImports) {

  public ChertClientConfig(URI endpoint, Duration requestTimeout, String appId, String accessKey, String secretKey,
      String env, Path localCachePath, Duration refreshInterval) {
    this(endpoint, requestTimeout, appId, accessKey, secretKey, env, localCachePath, refreshInterval,
        Collections.emptySet());
  }

  public ChertClientConfig {
    Objects.requireNonNull(endpoint, "endpoint must not be null");
    Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
    Objects.requireNonNull(appId, "appId must not be null");
    Objects.requireNonNull(accessKey, "accessKey must not be null");
    Objects.requireNonNull(secretKey, "secretKey must not be null");
    Objects.requireNonNull(env, "env must not be null");
    Objects.requireNonNull(refreshInterval, "refreshInterval must not be null");
    Objects.requireNonNull(configImports, "configImports must not be null");

    if (requestTimeout.isZero() || requestTimeout.isNegative()) {
      throw new IllegalArgumentException("requestTimeout must be greater than zero");
    }
    if (refreshInterval.isZero() || refreshInterval.isNegative()) {
      throw new IllegalArgumentException("refreshInterval must be greater than zero");
    }
  }

}
