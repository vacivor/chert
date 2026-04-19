package io.vacivor.chert.micronaut;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Internal;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.vacivor.chert.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@Requires(beans = {ChertClient.class, ChertConfigurationProperties.class})
@Internal
public class ChertRefreshManager {

    private static final Logger LOG = LoggerFactory.getLogger(ChertRefreshManager.class);

    private final ChertClient chertClient;
    private final Environment environment;
    private final ChertConfigurationProperties props;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;
    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();
    private final PropertiesPropertySourceLoader propertiesLoader = new PropertiesPropertySourceLoader();

    private final Set<String> changedConfigIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "chert-refresh-manager");
        thread.setDaemon(true);
        return thread;
    });

    public ChertRefreshManager(ChertClient chertClient, Environment environment, ChertConfigurationProperties props, ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this.chertClient = chertClient;
        this.environment = environment;
        this.props = props;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        // 获取所有配置资源
        List<String> configImports = props.getImports();
        
        for (String configId : configImports) {
            chertClient.addListener(configId, content -> {
                LOG.info("Detected Chert configuration change for [{}].", configId);
                refresh(configId);
            });
        }
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    public void refresh(String configId) {
        changedConfigIds.add(configId);
        if (refreshScheduled.compareAndSet(false, true)) {
            executor.execute(this::executeRefresh);
        }
    }

    private synchronized void executeRefresh() {
        refreshScheduled.set(false);
        Set<String> idsToRefresh = new HashSet<>(changedConfigIds);
        changedConfigIds.removeAll(idsToRefresh);

        if (idsToRefresh.isEmpty()) {
            return;
        }

        boolean refreshed = false;
        for (String configId : idsToRefresh) {
            try {
                ChertConfigResponse response = chertClient.fetchConfig(configId);
                if (response.type() == ConfigType.CONTENT) {
                    LOG.info("Refreshing Chert configuration for [{}].", configId);
                    updateEnvironment(configId, response);
                    refreshed = true;
                } else if (response.type() == ConfigType.ENTRIES) {
                    LOG.debug("Skipping refresh for ENTRIES type configuration [{}].", configId);
                }
            } catch (Exception e) {
                LOG.error("Failed to refresh Chert configuration for [{}]: {}", configId, e.getMessage());
            }
        }

        if (refreshed) {
            // 发布刷新事件，触发 @Refreshable Bean 重新绑定
            LOG.info("Publishing Micronaut RefreshEvent.");
            eventPublisher.publishEvent(new RefreshEvent());
        }
    }

    private void updateEnvironment(String configId, ChertConfigResponse response) {
        String name = "chert:" + configId;
        try {
            Map<String, Object> newMap = null;
            if (response.format() == ConfigFormat.YAML || configId.endsWith(".yml") || configId.endsWith(".yaml")) {
                newMap = yamlLoader.read(name, new ByteArrayInputStream(response.content().getBytes()));
            } else if (response.format() == ConfigFormat.PROPERTIES || configId.endsWith(".properties")) {
                newMap = propertiesLoader.read(name, new ByteArrayInputStream(response.content().getBytes()));
            }

            if (newMap != null) {
                // Micronaut 允许我们在运行时修改 Environment 中的 PropertySource
                // 虽然直接替换比较难（Environment 接口没有 remove），
                // 但我们可以通过优先级注入新的 MapPropertySource
                environment.addPropertySource(PropertySource.of(name, newMap));
            }
        } catch (Exception e) {
            LOG.error("Failed to parse configuration during refresh for [{}]: {}", configId, e.getMessage());
        }
    }
}
