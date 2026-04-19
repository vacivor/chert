package io.vacivor.chert.micronaut;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;
import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertClientConfig;
import io.vacivor.chert.client.ChertClients;
import jakarta.inject.Singleton;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Factory
public class ChertClientFactory {

    @Singleton
    @Bean(preDestroy = "close")
    public ChertClient chertClient(ChertConfigurationProperties props, Environment environment) {
        // 合并来自 bootstrap 加载阶段的 configImports
        List<String> configImports = environment.getProperty("chert.client.imports", List.class).orElse(Collections.emptyList());

        ChertClientConfig config = new ChertClientConfig(
                URI.create(props.getEndpoint()),
                props.getRequestTimeout(),
                props.getAppId() != null ? props.getAppId() : "default-app",
                props.getAccessKey() != null ? props.getAccessKey() : "default-key",
                props.getSecretKey() != null ? props.getSecretKey() : "default-secret",
                props.getEnv(),
                props.getLocalCachePath().map(Path::of).orElse(null),
                props.getRefreshInterval(),
                new HashSet<>(configImports)
        );

        return ChertClients.create(config);
    }
}
