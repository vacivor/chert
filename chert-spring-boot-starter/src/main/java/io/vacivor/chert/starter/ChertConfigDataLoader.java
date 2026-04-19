package io.vacivor.chert.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertClientConfig;
import io.vacivor.chert.client.ChertClients;
import io.vacivor.chert.client.ChertConfigResponse;
import io.vacivor.chert.client.ConfigFormat;
import io.vacivor.chert.client.ConfigType;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChertConfigDataLoader implements ConfigDataLoader<ChertConfigDataResource> {

  private static final Set<String> IMPORTED_CONFIG_NAMES = ConcurrentHashMap.newKeySet();

  public static Set<String> getImportedConfigNames() {
    return new HashSet<>(IMPORTED_CONFIG_NAMES);
  }

  private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();
  private final PropertiesPropertySourceLoader propertiesLoader = new PropertiesPropertySourceLoader();

  @Override
  public ConfigData load(ConfigDataLoaderContext context, ChertConfigDataResource resource)
      throws IOException, ConfigDataResourceNotFoundException {
    
    ChertClientConfig clientConfig = getClientConfig(context, resource.getConfigName());
    try (ChertClient client = ChertClients.create(clientConfig)) {
      ChertConfigResponse response;
      try {
        response = client.fetchConfig();
      } catch (Exception ex) {
        throw new ConfigDataResourceNotFoundException(resource, ex);
      }
      
      if (response.type() == ConfigType.ENTRIES) {
        System.err.println("[WARN] Config [" + resource.getConfigName() + "] is of type ENTRIES, which is not supported in spring.config.import. Use @ChertConfig or addListener instead.");
        return new ConfigData(Collections.emptyList());
      }
      
      IMPORTED_CONFIG_NAMES.add(resource.getConfigName());

      PropertySourceLoader loader = getLoader(resource.getConfigName(), response.format());
      List<PropertySource<?>> propertySources = loader.load("chert:" + resource.getConfigName(),
          new ByteArrayResource(response.content().getBytes()));

      return new ConfigData(propertySources);
    }
  }

  private ChertClientConfig getClientConfig(ConfigDataLoaderContext context, String configName) {
    Binder binder = context.getBootstrapContext().get(Binder.class);
    ChertClientProperties props = binder.bind("chert.client", ChertClientProperties.class)
        .orElseGet(ChertClientProperties::new);
    
    return new ChertClientConfig(
        URI.create(props.getEndpoint()),
        props.getRequestTimeout(),
        props.getAppId() != null ? props.getAppId() : "default-app",
        props.getAccessKey() != null ? props.getAccessKey() : "default-key",
        props.getSecretKey() != null ? props.getSecretKey() : "default-secret",
        props.getEnv(),
        props.getLocalCachePath() != null ? java.nio.file.Path.of(props.getLocalCachePath()) : null,
        props.getRefreshInterval()
    );
  }

  private PropertySourceLoader getLoader(String configName, ConfigFormat format) {
    if (format == ConfigFormat.YAML || configName.endsWith(".yml") || configName.endsWith(".yaml")) {
      return yamlLoader;
    }
    return propertiesLoader;
  }

}
