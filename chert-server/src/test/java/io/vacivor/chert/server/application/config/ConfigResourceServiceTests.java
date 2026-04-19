package io.vacivor.chert.server.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.common.ConfigFormat;
import io.vacivor.chert.server.common.ConfigType;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.error.ChertException;
import io.vacivor.chert.server.error.ConfigResourceErrorCode;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigResourceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigResourceServiceTests {

  @Mock
  private ConfigResourceRepository configResourceRepository;

  @Mock
  private ApplicationService applicationService;

  @InjectMocks
  private ConfigResourceService configResourceService;

  @Test
  void shouldUseConfigResourceSpecificCodeWhenNameAlreadyExists() {
    ConfigResource configResource = new ConfigResource();
    configResource.setApplicationId(1L);
    configResource.setName("application.yml");
    configResource.setType(ConfigType.CONTENT);
    configResource.setFormat(ConfigFormat.YAML);
    when(applicationService.get(1L)).thenReturn(new Application());
    when(configResourceRepository.findByApplicationIdAndName(1L, "application.yml"))
        .thenReturn(Optional.of(new ConfigResource()));

    assertThatThrownBy(() -> configResourceService.create(configResource))
        .isInstanceOf(ChertException.class)
        .extracting(exception -> ((ChertException) exception).getErrorCode())
        .isEqualTo(ConfigResourceErrorCode.CONFIG_RESOURCE_NAME_DUPLICATED);

    verify(configResourceRepository, never()).save(configResource);
  }

  @Test
  void shouldUseConfigResourceSpecificCodeWhenResourceDoesNotExist() {
    when(configResourceRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> configResourceService.get(42L))
        .isInstanceOf(ChertException.class)
        .satisfies(exception -> {
          ChertException chertException = (ChertException) exception;
          assertThat(chertException.getErrorCode()).isEqualTo(ConfigResourceErrorCode.CONFIG_RESOURCE_NOT_FOUND);
        });
  }
}
