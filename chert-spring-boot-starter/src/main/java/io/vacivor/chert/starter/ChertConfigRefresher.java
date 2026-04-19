package io.vacivor.chert.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfigResponse;
import io.vacivor.chert.client.ConfigFormat;
import io.vacivor.chert.client.ConfigType;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.ReflectionUtils;

public class ChertConfigRefresher implements InitializingBean, DisposableBean, ApplicationContextAware, BeanPostProcessor {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^:}]+)");
  private final ChertClient chertClient;
  private final ConfigurableEnvironment environment;
  private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();
  private final PropertiesPropertySourceLoader propertiesLoader = new PropertiesPropertySourceLoader();
  private final Map<String, Set<ValueElement>> keyToValueElements = new ConcurrentHashMap<>();
  private final Map<String, Set<Object>> prefixToConfigBeans = new ConcurrentHashMap<>();
  private final Set<String> changedConfigNames = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);
  private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
    Thread thread = new Thread(r, "chert-config-refresher");
    thread.setDaemon(true);
    return thread;
  });

  private ApplicationContext applicationContext;

  public ChertConfigRefresher(ChertClient chertClient, ConfigurableEnvironment environment) {
    this.chertClient = chertClient;
    this.environment = environment;
  }

  @Override
  public void destroy() {
    executor.shutdownNow();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    Class<?> clazz = bean.getClass();
    // Fields
    ReflectionUtils.doWithFields(clazz, field -> {
      Value value = field.getAnnotation(Value.class);
      if (value != null) {
        registerValueElement(new FieldValueElement(bean, field, value.value()));
      }
    });
    // Methods (Setters)
    ReflectionUtils.doWithMethods(clazz, method -> {
      Value value = method.getAnnotation(Value.class);
      if (value != null && method.getParameterCount() == 1) {
        registerValueElement(new MethodValueElement(bean, method, value.value()));
      }
    });
    // ConfigurationProperties
    ConfigurationProperties configAnn = AnnotationUtils.findAnnotation(clazz, ConfigurationProperties.class);
    if (configAnn != null) {
      prefixToConfigBeans.computeIfAbsent(configAnn.prefix(), k -> ConcurrentHashMap.newKeySet()).add(bean);
    }
    return bean;
  }

  private void registerValueElement(ValueElement element) {
    Set<String> keys = extractKeys(element.getExpression());
    for (String key : keys) {
      keyToValueElements.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(element);
    }
  }

  private Set<String> extractKeys(String expression) {
    Set<String> keys = new HashSet<>();
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);
    while (matcher.find()) {
      keys.add(matcher.group(1).trim());
    }
    return keys;
  }

  @Override
  public void afterPropertiesSet() {
    Set<String> configNames = ChertConfigDataLoader.getImportedConfigNames();
    for (String configName : configNames) {
      chertClient.addListener(configName, content -> refresh(configName));
    }
  }

  public void refresh(String configName) {
    changedConfigNames.add(configName);
    if (refreshScheduled.compareAndSet(false, true)) {
      executor.execute(this::executeRefresh);
    }
  }

  private synchronized void executeRefresh() {
    refreshScheduled.set(false);
    Set<String> namesToRefresh = new HashSet<>(changedConfigNames);
    changedConfigNames.removeAll(namesToRefresh);

    if (namesToRefresh.isEmpty()) {
      return;
    }

    try {
      Set<String> changedKeys = new HashSet<>();
      for (String configName : namesToRefresh) {
        ChertConfigResponse response = chertClient.fetchConfig(configName);
        if (response.type() == ConfigType.CONTENT) {
          Set<String> keys = updateEnvironmentAndGetChangedKeys(configName, response);
          changedKeys.addAll(keys);
        }
      }

      if (!changedKeys.isEmpty()) {
        rebindConfigurationProperties(changedKeys);
        refreshValueBeans(changedKeys);
      }
    } catch (Exception e) {
      System.err.println("Failed to refresh Chert configuration: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private Set<String> updateEnvironmentAndGetChangedKeys(String configName, ChertConfigResponse response) throws IOException {
    String propertySourceName = "chert:" + configName;
    MutablePropertySources sources = environment.getPropertySources();
    PropertySource<?> oldSource = sources.get(propertySourceName);

    PropertySourceLoader loader = getLoader(configName, response.format());
    List<PropertySource<?>> loadedSources = loader.load(propertySourceName,
        new ByteArrayResource(response.content().getBytes()));
    
    Set<String> changedKeys = new HashSet<>();
    // Usually it's just one, but handle multiple if exists (like YAML documents)
    for (PropertySource<?> newSource : loadedSources) {
      changedKeys.addAll(findChangedKeys(oldSource, newSource));
      replacePropertySource(sources, newSource);
    }
    return changedKeys;
  }

  private Set<String> findChangedKeys(PropertySource<?> oldSource, PropertySource<?> newSource) {
    Set<String> changedKeys = new HashSet<>();
    if (newSource instanceof EnumerablePropertySource<?> newEnumerable) {
      for (String key : newEnumerable.getPropertyNames()) {
        Object newValue = newEnumerable.getProperty(key);
        Object oldValue = (oldSource != null) ? oldSource.getProperty(key) : null;
        if (!Objects.equals(newValue, oldValue)) {
          changedKeys.add(key);
        }
      }
    }
    
    // Also check for deleted keys
    if (oldSource instanceof EnumerablePropertySource<?> oldEnumerable) {
      for (String key : oldEnumerable.getPropertyNames()) {
        if (newSource.getProperty(key) == null) {
          changedKeys.add(key);
        }
      }
    }
    
    return changedKeys;
  }

  private void replacePropertySource(MutablePropertySources sources, PropertySource<?> newSource) {
    if (sources.contains(newSource.getName())) {
      sources.replace(newSource.getName(), newSource);
    } else {
      // Find where to insert. For simplicity, we just add it first if it doesn't exist
      // But it SHOULD exist because it was loaded by ChertConfigDataLoader
      sources.addFirst(newSource);
    }
  }

  private void rebindConfigurationProperties(Set<String> changedKeys) {
    Binder binder = Binder.get(environment);
    Set<Object> beansToRebind = new HashSet<>();
    
    for (Map.Entry<String, Set<Object>> entry : prefixToConfigBeans.entrySet()) {
      String prefix = entry.getKey();
      for (String changedKey : changedKeys) {
        if (changedKey.startsWith(prefix)) {
          beansToRebind.addAll(entry.getValue());
          break;
        }
      }
    }

    // Fallback for beans missed by BeanPostProcessor (useful in tests)
    if (beansToRebind.isEmpty() && applicationContext != null) {
      Map<String, Object> allConfigBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
      for (Object bean : allConfigBeans.values()) {
        ConfigurationProperties ann = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
        if (ann != null) {
          String prefix = ann.prefix();
          for (String changedKey : changedKeys) {
            if (changedKey.startsWith(prefix)) {
              beansToRebind.add(bean);
              break;
            }
          }
        }
      }
    }

    for (Object bean : beansToRebind) {
      ConfigurationProperties ann = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
      if (ann != null) {
        binder.bind(ann.prefix(), Bindable.ofInstance(bean));
      }
    }
  }

  private void refreshValueBeans(Set<String> changedKeys) {
    Set<ValueElement> elementsToRefresh = new HashSet<>();
    for (String changedKey : changedKeys) {
      Set<ValueElement> elements = keyToValueElements.get(changedKey);
      if (elements != null) {
        elementsToRefresh.addAll(elements);
      }
    }

    // Fallback for beans missed by BeanPostProcessor
    if (elementsToRefresh.isEmpty() && applicationContext != null) {
        // Here we don't have an easy way to scan all beans for @Value efficiently without BPP
        // So we just iterate through all known elements if any (but they are empty in the test)
    }

    for (ValueElement element : elementsToRefresh) {
      element.update(environment);
    }
  }

  private interface ValueElement {
    void update(ConfigurableEnvironment environment);
    String getExpression();
  }

  private static class FieldValueElement implements ValueElement {
    private final Object bean;
    private final Field field;
    private final String expression;

    FieldValueElement(Object bean, Field field, String expression) {
      this.bean = bean;
      this.field = field;
      this.expression = expression;
    }

    @Override
    public void update(ConfigurableEnvironment environment) {
      try {
        String valueStr = environment.resolvePlaceholders(expression);
        ReflectionUtils.makeAccessible(field);
        Object value = environment.getConversionService().convert(valueStr, field.getType());
        field.set(bean, value);
      } catch (Exception ignored) {
      }
    }

    @Override
    public String getExpression() {
      return expression;
    }
  }

  private static class MethodValueElement implements ValueElement {
    private final Object bean;
    private final Method method;
    private final String expression;

    MethodValueElement(Object bean, Method method, String expression) {
      this.bean = bean;
      this.method = method;
      this.expression = expression;
    }

    @Override
    public void update(ConfigurableEnvironment environment) {
      try {
        String valueStr = environment.resolvePlaceholders(expression);
        ReflectionUtils.makeAccessible(method);
        Object value = environment.getConversionService().convert(valueStr, method.getParameterTypes()[0]);
        method.invoke(bean, value);
      } catch (Exception ignored) {
      }
    }

    @Override
    public String getExpression() {
      return expression;
    }
  }

  private PropertySourceLoader getLoader(String configName, ConfigFormat format) {
    if (format == ConfigFormat.YAML || configName.endsWith(".yml") || configName.endsWith(".yaml")) {
      return yamlLoader;
    }
    return propertiesLoader;
  }
}
