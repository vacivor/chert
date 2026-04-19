package io.vacivor.chert.micronaut;

import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.core.annotation.Internal;
import io.micronaut.discovery.config.ConfigurationClient;
import io.vacivor.chert.client.*;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Singleton
@Internal
public class ChertConfigurationClient implements ConfigurationClient {

    private static final Logger LOG = LoggerFactory.getLogger(ChertConfigurationClient.class);

    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();
    private final PropertiesPropertySourceLoader propertiesLoader = new PropertiesPropertySourceLoader();

    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        if (!environment.getProperty("chert.client.enabled", Boolean.class).orElse(true)) {
            return subscriber -> subscriber.onSubscribe(new Subscription() {
                @Override public void request(long n) { subscriber.onComplete(); }
                @Override public void cancel() {}
            });
        }

        ChertConfigurationProperties props = environment.getProperty(ChertConfigurationProperties.PREFIX, ChertConfigurationProperties.class)
                .orElse(new ChertConfigurationProperties());

        ChertClientConfig config = new ChertClientConfig(
                URI.create(props.getEndpoint()),
                props.getRequestTimeout(),
                props.getAppId() != null ? props.getAppId() : "default-app",
                props.getAccessKey() != null ? props.getAccessKey() : "default-key",
                props.getSecretKey() != null ? props.getSecretKey() : "default-secret",
                props.getEnv(),
                props.getLocalCachePath().map(Path::of).orElse(null),
                props.getRefreshInterval(),
                new HashSet<>(props.getImports())
        );

        List<PropertySource> propertySources = new ArrayList<>();
        try (ChertClient client = ChertClients.create(config)) {
            for (String configId : props.getImports()) {
                try {
                    ChertConfigResponse response = client.fetchConfig(configId);
                    if (response.type() == ConfigType.ENTRIES) {
                        LOG.warn("Config [{}] is of type ENTRIES, skipping in Micronaut bootstrap.", configId);
                        continue;
                    }
                    
                    PropertySource ps = loadPropertySource(configId, response);
                    if (ps != null) {
                        propertySources.add(ps);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to load Chert configuration for [{}]: {}", configId, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize Chert client: {}", e.getMessage());
        }

        return subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (n > 0) {
                    for (PropertySource ps : propertySources) {
                        subscriber.onNext(ps);
                    }
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
            }
        });
    }

    private PropertySource loadPropertySource(String configId, ChertConfigResponse response) {
        String name = "chert:" + configId;
        if (response.format() == ConfigFormat.YAML || configId.endsWith(".yml") || configId.endsWith(".yaml")) {
            try {
                Map<String, Object> map = yamlLoader.read(name, new ByteArrayInputStream(response.content().getBytes()));
                return PropertySource.of(name, map);
            } catch (IOException e) {
                LOG.error("Failed to parse YAML configuration for [{}]: {}", configId, e.getMessage());
            }
        } else if (response.format() == ConfigFormat.PROPERTIES || configId.endsWith(".properties")) {
            try {
                Map<String, Object> map = propertiesLoader.read(name, new ByteArrayInputStream(response.content().getBytes()));
                return PropertySource.of(name, map);
            } catch (IOException e) {
                LOG.error("Failed to parse properties configuration for [{}]: {}", configId, e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "Chert Configuration Client";
    }
}
