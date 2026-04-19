package io.vacivor.chert.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfig;
import io.vacivor.chert.client.ChertConfigListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class ChertConfigBeanPostProcessor implements BeanPostProcessor {

  private final ChertClient chertClient;

  public ChertConfigBeanPostProcessor(ChertClient chertClient) {
    this.chertClient = chertClient;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    ReflectionUtils.doWithMethods(bean.getClass(), method -> {
      ChertConfig ann = AnnotationUtils.findAnnotation(method, ChertConfig.class);
      if (ann != null) {
        registerListener(bean, method, ann);
      }
    });
    return bean;
  }

  private void registerListener(Object bean, Method method, ChertConfig ann) {
    String resource = ann.resource();
    ChertConfigListener listener = content -> {
      ReflectionUtils.makeAccessible(method);
      if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(String.class)) {
        ReflectionUtils.invokeMethod(method, bean, content);
      } else if (method.getParameterCount() == 0) {
        ReflectionUtils.invokeMethod(method, bean);
      }
    };

    if (resource != null && !resource.isBlank()) {
      chertClient.addListener(resource, listener);
    } else {
      chertClient.addListener(listener);
    }
  }
}
