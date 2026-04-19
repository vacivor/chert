package io.vacivor.chert.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChertServerApplication {

  static void main(String[] args) {
    SpringApplication.run(ChertServerApplication.class, args);
  }
}
