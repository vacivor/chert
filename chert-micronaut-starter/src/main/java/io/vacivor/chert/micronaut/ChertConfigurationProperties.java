package io.vacivor.chert.micronaut;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.NonNull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties(ChertConfigurationProperties.PREFIX)
public class ChertConfigurationProperties {
    public static final String PREFIX = "chert.client";

    private String endpoint = "http://localhost:8080";
    private Duration requestTimeout = Duration.ofSeconds(2);
    private String appId;
    private String accessKey;
    private String secretKey;
    private String env = "default";
    private String localCachePath;
    private Duration refreshInterval = Duration.ofMinutes(5);
    private boolean enabled = true;
    private List<String> imports = Collections.singletonList("application.yml");

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

    public Optional<String> getLocalCachePath() {
        return Optional.ofNullable(localCachePath);
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }
}
