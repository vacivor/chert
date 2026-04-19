package io.vacivor.chert.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfigResponse;
import io.vacivor.chert.client.ConfigFormat;
import io.vacivor.chert.client.ConfigType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ChertConfigDataTests {

  @Test
  void loaderShouldThrowResourceNotFoundOnClientError() throws IOException {
    ChertConfigDataLoader loader = new ChertConfigDataLoader();
    ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class, RETURNS_DEEP_STUBS);
    ChertConfigDataResource resource = new ChertConfigDataResource("error-config");

    Binder binder = mock(Binder.class);
    when(context.getBootstrapContext().get(Binder.class)).thenReturn(binder);
    
    BindResult<ChertClientProperties> bindResult = mock(BindResult.class);
    when(binder.bind(eq("chert.client"), eq(ChertClientProperties.class))).thenReturn(bindResult);
    when(bindResult.orElseGet(any())).thenReturn(new ChertClientProperties());

    ChertClient client = mock(ChertClient.class);
    when(client.fetchConfig()).thenThrow(new RuntimeException("Server unreachable"));

    try (MockedStatic<io.vacivor.chert.client.ChertClients> chertClients = mockStatic(io.vacivor.chert.client.ChertClients.class)) {
      chertClients.when(() -> io.vacivor.chert.client.ChertClients.create(any())).thenReturn(client);

      assertThatThrownBy(() -> loader.load(context, resource))
          .isInstanceOf(ConfigDataResourceNotFoundException.class)
          .hasMessageContaining("error-config");
    }
  }

  @Test
  void resolverShouldSupportChertPrefix() {
    ChertConfigDataLocationResolver resolver = new ChertConfigDataLocationResolver();
    ConfigDataLocation location = ConfigDataLocation.of("chert:application.yml");
    ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

    assertThat(resolver.isResolvable(context, location)).isTrue();
    
    List<ChertConfigDataResource> resources = resolver.resolve(context, location);
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getConfigName()).isEqualTo("application.yml");
  }

  @Test
  void resolverShouldSupportChertDoubleSlashPrefix() {
    ChertConfigDataLocationResolver resolver = new ChertConfigDataLocationResolver();
    ConfigDataLocation location = ConfigDataLocation.of("chert://application.yml");
    ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

    assertThat(resolver.isResolvable(context, location)).isTrue();

    List<ChertConfigDataResource> resources = resolver.resolve(context, location);
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getConfigName()).isEqualTo("application.yml");
  }

  @Test
  void loaderShouldSkipEntriesType() throws IOException {
    ChertConfigDataLoader loader = new ChertConfigDataLoader();
    ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class, RETURNS_DEEP_STUBS);
    ChertConfigDataResource resource = new ChertConfigDataResource("entries-config");

    Binder binder = mock(Binder.class);
    when(context.getBootstrapContext().get(Binder.class)).thenReturn(binder);
    
    BindResult<ChertClientProperties> bindResult = mock(BindResult.class);
    when(binder.bind(eq("chert.client"), eq(ChertClientProperties.class))).thenReturn(bindResult);
    when(bindResult.orElseGet(any())).thenReturn(new ChertClientProperties());

    ChertClient client = mock(ChertClient.class);
    ChertConfigResponse response = new ChertConfigResponse("[]", Instant.now(), ConfigType.ENTRIES, ConfigFormat.JSON);
    when(client.fetchConfig()).thenReturn(response);

    try (MockedStatic<io.vacivor.chert.client.ChertClients> chertClients = mockStatic(io.vacivor.chert.client.ChertClients.class)) {
      chertClients.when(() -> io.vacivor.chert.client.ChertClients.create(any())).thenReturn(client);

      ConfigData configData = loader.load(context, resource);

      assertThat(configData.getPropertySources()).isEmpty();
      assertThat(ChertConfigDataLoader.getImportedConfigNames()).doesNotContain("entries-config");
    }
  }

}
