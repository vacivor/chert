package io.vacivor.chert.server.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.domain.config.ConfigContent;
import io.vacivor.chert.server.domain.config.ConfigRelease;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigDiffServiceTests {

  @Mock
  private ConfigContentService configContentService;
  @Mock
  private ConfigReleaseService configReleaseService;

  @InjectMocks
  private ConfigDiffService configDiffService;

  @Test
  void shouldDiffDraftWithLatestRelease() {
    ConfigContent draft = new ConfigContent();
    draft.setContent("new-content");
    when(configContentService.findLatest(1L, 1L)).thenReturn(Optional.of(draft));

    ConfigRelease latestRelease = new ConfigRelease();
    latestRelease.setSnapshot("old-content");
    when(configReleaseService.findLatest(1L, 1L)).thenReturn(Optional.of(latestRelease));

    Optional<ConfigDiffService.DiffResult> resultOpt = configDiffService.diffDraftWithLatestRelease(1L, 1L);

    assertThat(resultOpt).isPresent();
    assertThat(resultOpt.get().oldContent()).isEqualTo("old-content");
    assertThat(resultOpt.get().newContent()).isEqualTo("new-content");
  }

  @Test
  void shouldReturnEmptyWhenBothEmpty() {
    when(configContentService.findLatest(1L, 1L)).thenReturn(Optional.empty());
    when(configReleaseService.findLatest(1L, 1L)).thenReturn(Optional.empty());

    Optional<ConfigDiffService.DiffResult> resultOpt = configDiffService.diffDraftWithLatestRelease(1L, 1L);

    assertThat(resultOpt).isEmpty();
  }
}
