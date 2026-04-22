package io.vacivor.chert.server.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.application.environment.EnvironmentService;
import io.vacivor.chert.server.common.ConfigType;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.config.ConfigContent;
import io.vacivor.chert.server.domain.config.ConfigRelease;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigReleaseHistoryRepository;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigReleaseRepository;
import io.vacivor.chert.server.infrastructure.persistence.config.ReleaseMessageRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigReleaseServiceTests {

  @Mock
  private ConfigReleaseRepository configReleaseRepository;
  @Mock
  private ConfigReleaseHistoryRepository configReleaseHistoryRepository;
  @Mock
  private ReleaseMessageRepository releaseMessageRepository;
  @Mock
  private ConfigContentService configContentService;
  @Mock
  private ConfigEntryService configEntryService;
  @Mock
  private ConfigResourceService configResourceService;
  @Mock
  private AuditLogService auditLogService;
  @Mock
  private ApplicationService applicationService;
  @Mock
  private EnvironmentService environmentService;
  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private ConfigReleaseService configReleaseService;

  @Test
  void shouldPublishNewVersionWhenContentChanged() {
    ConfigResource resource = new ConfigResource();
    resource.setId(1L);
    resource.setApplicationId(1L);
    resource.setName("app.yml");
    resource.setType(ConfigType.CONTENT);
    when(configResourceService.get(1L)).thenReturn(resource);

    Application app = new Application();
    app.setId(1L);
    app.setAppId("test-app");
    when(applicationService.get(1L)).thenReturn(app);

    Environment env = new Environment();
    env.setId(1L);
    env.setCode("dev");
    when(environmentService.get(1L)).thenReturn(env);

    ConfigContent content = new ConfigContent();
    content.setContent("new-content");
    when(configContentService.findLatest(1L, 1L)).thenReturn(Optional.of(content));

    ConfigRelease latestRelease = new ConfigRelease();
    latestRelease.setId(10L);
    latestRelease.setSnapshot("old-content");
    latestRelease.setVersion(5L);
    when(configReleaseRepository.findTopByConfigResourceIdAndEnvironmentIdOrderByVersionDesc(1L, 1L))
        .thenReturn(Optional.of(latestRelease));

    when(configReleaseRepository.save(any())).thenAnswer(invocation -> {
      ConfigRelease r = invocation.getArgument(0);
      r.setId(11L);
      return r;
    });

    ConfigRelease result = configReleaseService.publish(1L, 1L, "update");

    assertThat(result.getVersion()).isEqualTo(6L);
    assertThat(result.getSnapshot()).isEqualTo("new-content");
    verify(configReleaseRepository, times(1)).save(any());
    verify(configReleaseHistoryRepository, times(1)).save(any());
  }

  @Test
  void shouldNotPublishWhenContentIsSame() {
    ConfigResource resource = new ConfigResource();
    resource.setId(1L);
    resource.setType(ConfigType.CONTENT);
    when(configResourceService.get(1L)).thenReturn(resource);

    ConfigContent content = new ConfigContent();
    content.setContent("same-content");
    when(configContentService.findLatest(1L, 1L)).thenReturn(Optional.of(content));

    ConfigRelease latestRelease = new ConfigRelease();
    latestRelease.setSnapshot("same-content");
    latestRelease.setVersion(5L);
    when(configReleaseRepository.findTopByConfigResourceIdAndEnvironmentIdOrderByVersionDesc(1L, 1L))
        .thenReturn(Optional.of(latestRelease));

    ConfigRelease result = configReleaseService.publish(1L, 1L, "no-op");

    assertThat(result).isEqualTo(latestRelease);
    verify(configReleaseRepository, never()).save(any());
  }

  @Test
  void shouldRollbackToTargetRelease() {
    ConfigRelease targetRelease = new ConfigRelease();
    targetRelease.setId(50L);
    targetRelease.setConfigResourceId(1L);
    targetRelease.setEnvironmentId(1L);
    targetRelease.setSnapshot("target-content");
    targetRelease.setVersion(2L);

    ConfigResource resource = new ConfigResource();
    resource.setId(1L);
    resource.setApplicationId(1L);
    resource.setName("app.yml");
    when(configResourceService.get(1L)).thenReturn(resource);

    Application app = new Application();
    app.setId(1L);
    app.setAppId("test-app");
    when(applicationService.get(1L)).thenReturn(app);

    Environment env = new Environment();
    env.setId(1L);
    env.setCode("dev");
    when(environmentService.get(1L)).thenReturn(env);

    ConfigRelease latestRelease = new ConfigRelease();
    latestRelease.setId(60L);
    latestRelease.setConfigResourceId(1L);
    latestRelease.setEnvironmentId(1L);
    latestRelease.setSnapshot("current-content");
    latestRelease.setVersion(5L);

    when(configReleaseRepository.findById(50L)).thenReturn(Optional.of(targetRelease));
    when(configReleaseRepository.findTopByConfigResourceIdAndEnvironmentIdOrderByVersionDesc(1L, 1L))
        .thenReturn(Optional.of(latestRelease));

    when(configReleaseRepository.save(any())).thenAnswer(invocation -> {
      ConfigRelease r = invocation.getArgument(0);
      r.setId(61L);
      return r;
    });

    ConfigRelease result = configReleaseService.rollback(50L, "rollback to v2");

    assertThat(result.getVersion()).isEqualTo(6L);
    assertThat(result.getSnapshot()).isEqualTo("target-content");
    assertThat(result.getType()).isEqualTo("ROLLBACK");
    verify(releaseMessageRepository, times(1)).save(any());
  }
}
