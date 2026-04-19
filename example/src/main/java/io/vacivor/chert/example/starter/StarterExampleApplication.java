package io.vacivor.chert.example.starter;

import io.vacivor.chert.client.ChertConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StarterExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(StarterExampleApplication.class, args);
  }

  @ChertConfig
  public void onMainConfigUpdate(String content) {
    System.out.println("[Listener] Main config (application.yml) updated: " + content);
  }

  @ChertConfig(resource = "database.properties")
  public void onDatabaseConfigUpdate(String content) {
    System.out.println("[Listener] Database config updated: " + content);
  }

}
