package io.vacivor.chert.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertClientConfig;
import io.vacivor.chert.client.ChertClients;
import java.net.URI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

@AutoConfiguration
@ConditionalOnClass(ChertClient.class)
@EnableConfigurationProperties(ChertClientProperties.class)
public class ChertAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ChertClient chertClient(ChertClientProperties properties) {
    ChertClientConfig config = new ChertClientConfig(
        URI.create(properties.getEndpoint()),
        properties.getRequestTimeout(),
        properties.getAppId() != null ? properties.getAppId() : "default-app",
        properties.getAccessKey() != null ? properties.getAccessKey() : "default-key",
        properties.getSecretKey() != null ? properties.getSecretKey() : "default-secret",
        properties.getEnv(),
        properties.getLocalCachePath() != null ? java.nio.file.Path.of(properties.getLocalCachePath()) : null,
        properties.getRefreshInterval(),
        ChertConfigDataLoader.getImportedConfigNames());
    return ChertClients.create(config);
  }

  @Bean
  @ConditionalOnMissingBean
  public ChertConfigBeanPostProcessor chertConfigBeanPostProcessor(ChertClient chertClient) {
    return new ChertConfigBeanPostProcessor(chertClient);
  }

  @Bean
  @ConditionalOnMissingBean
  public ChertConfigRefresher chertConfigRefresher(ChertClient chertClient, ConfigurableEnvironment environment) {
    return new ChertConfigRefresher(chertClient, environment);
  }

}
