package io.vacivor.chert.micronaut;

import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfig;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class ChertConfigMethodProcessor implements ExecutableMethodProcessor<ChertConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(ChertConfigMethodProcessor.class);

    private final ChertClient chertClient;
    private final BeanContext beanContext;

    public ChertConfigMethodProcessor(ChertClient chertClient, BeanContext beanContext) {
        this.chertClient = chertClient;
        this.beanContext = beanContext;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        ChertConfig annotation = method.synthesize(ChertConfig.class);

        String resource = annotation.resource();
        if (resource.isEmpty()) {
            // 默认回退到 application.yml (参考 Spring 实现)
            resource = "application.yml";
        }

        final String finalResource = resource;
        LOG.info("Registering @ChertConfig listener for resource [{}] on method [{}#{}]", 
                finalResource, beanDefinition.getBeanType().getSimpleName(), method.getMethodName());

        chertClient.addListener(finalResource, content -> {
            try {
                // 在 Micronaut 中，我们通过 BeanContext 获取 Bean 实例并调用方法
                Object bean = beanContext.getBean(beanDefinition.getBeanType());
                
                // 根据参数类型调用方法
                Class<?>[] parameterTypes = method.getArgumentTypes();
                ExecutableMethod<Object, Object> executableMethod = (ExecutableMethod<Object, Object>) method;
                if (parameterTypes.length == 0) {
                    executableMethod.invoke(bean);
                } else if (parameterTypes.length == 1 && parameterTypes[0].equals(String.class)) {
                    executableMethod.invoke(bean, content);
                } else {
                    LOG.warn("Unsupported parameter signature for @ChertConfig method: {}", method.getMethodName());
                }
            } catch (Exception e) {
                LOG.error("Failed to invoke @ChertConfig listener method [{}]: {}", method.getMethodName(), e.getMessage());
            }
        });
    }
}
