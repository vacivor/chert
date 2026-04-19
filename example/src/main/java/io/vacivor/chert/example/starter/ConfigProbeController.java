package io.vacivor.chert.example.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfigResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigProbeController {

  private final ChertClient chertClient;
  private final DemoProperties demoProperties;

  @Value("${demo.title:Default Title from @Value}")
  private String dynamicTitle;

  public ConfigProbeController(ChertClient chertClient, DemoProperties demoProperties) {
    this.chertClient = chertClient;
    this.demoProperties = demoProperties;
  }

  @GetMapping("/demo/properties")
  public DemoProperties properties() {
    return demoProperties;
  }

  @GetMapping("/demo/value")
  public String value() {
    return dynamicTitle;
  }

  @GetMapping("/demo/config")
  public ChertConfigResponse config() {
    return chertClient.fetchConfig();
  }

  @GetMapping("/demo/config/{configName}")
  public ChertConfigResponse specificConfig(@PathVariable String configName) {
    return chertClient.fetchConfig(configName);
  }

  @GetMapping("/demo/entries/{configName}")
  public Map<String, String> entries(@PathVariable String configName) {
    return chertClient.fetchEntries(configName);
  }

}
