package io.vacivor.chert.starter;

import org.springframework.boot.context.config.ConfigDataResource;

import java.util.Objects;

public class ChertConfigDataResource extends ConfigDataResource {

  private final String configName;

  public ChertConfigDataResource(String configName) {
    this.configName = configName;
  }

  public String getConfigName() {
    return configName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChertConfigDataResource that = (ChertConfigDataResource) o;
    return Objects.equals(configName, that.configName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configName);
  }

  @Override
  public String toString() {
    return "ChertConfigDataResource{" +
        "configName='" + configName + '\'' +
        '}';
  }
}
