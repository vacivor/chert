package io.vacivor.chert.example.micronaut;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicronautExampleApplication {

    private static final Logger LOG = LoggerFactory.getLogger(MicronautExampleApplication.class);

    public static void main(String[] args) {
        try (ApplicationContext context = Micronaut.run(MicronautExampleApplication.class, args)) {
            MicronautDemoBean demoBean = context.getBean(MicronautDemoBean.class);
            LOG.info("Micronaut App Name: {}", demoBean.getName());
            LOG.info("App Properties: title={}, version={}", 
                    demoBean.getAppProperties().getTitle(), 
                    demoBean.getAppProperties().getVersion());
        }
    }
}
