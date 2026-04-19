package io.vacivor.chert.starter;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;

import java.util.Collections;
import java.util.List;

public class ChertConfigDataLocationResolver implements ConfigDataLocationResolver<ChertConfigDataResource> {

  private static final String PREFIX = "chert:";

  @Override
  public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
    return location.hasPrefix(PREFIX);
  }

  @Override
  public List<ChertConfigDataResource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
      throws ConfigDataResourceNotFoundException {
    String configName = location.getNonPrefixedValue(PREFIX);
    if (configName.startsWith("//")) {
      configName = configName.substring(2);
    }
    return Collections.singletonList(new ChertConfigDataResource(configName));
  }

}
