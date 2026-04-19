package io.vacivor.chert.example.micronaut;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.context.scope.Refreshable;
import io.vacivor.chert.client.ChertConfig;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Refreshable
public class MicronautDemoBean {

    private static final Logger LOG = LoggerFactory.getLogger(MicronautDemoBean.class);

    private final AppProperties appProperties;

    @Value("${name:unknown}")
    private String name;

    public MicronautDemoBean(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @ChertConfig(resource = "application.yml")
    public void onConfigUpdate(String content) {
        LOG.info("Micronaut received config update via @ChertConfig (YML): {}", content);
    }

    @ChertConfig(resource = "database.properties")
    public void onDatabaseConfigUpdate(String content) {
        LOG.info("Micronaut received config update via @ChertConfig (Properties): {}", content);
    }

    public String getName() {
        return name;
    }

    public AppProperties getAppProperties() {
        return appProperties;
    }
}
