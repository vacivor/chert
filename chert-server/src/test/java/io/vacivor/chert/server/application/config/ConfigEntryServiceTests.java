package io.vacivor.chert.server.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigEntryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigEntryServiceTests {

  @Mock
  private ConfigEntryRepository configEntryRepository;

  @InjectMocks
  private ConfigEntryService configEntryService;

  @Test
  void shouldListEntries() {
    ConfigEntry entry = new ConfigEntry();
    entry.setKey("test-key");
    when(configEntryRepository.findByConfigResourceIdAndEnvironmentId(1L, 1L))
        .thenReturn(List.of(entry));

    List<ConfigEntry> result = configEntryService.list(1L, 1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo("test-key");
  }

  @Test
  void shouldSaveNewEntry() {
    ConfigEntry entry = new ConfigEntry();
    entry.setConfigResourceId(1L);
    entry.setEnvironmentId(1L);
    entry.setKey("new-key");
    entry.setValue("val");

    when(configEntryRepository.findByConfigResourceIdAndEnvironmentIdAndKey(1L, 1L, "new-key"))
        .thenReturn(Optional.empty());
    when(configEntryRepository.save(any())).thenReturn(entry);

    ConfigEntry result = configEntryService.save(entry);

    assertThat(result.getKey()).isEqualTo("new-key");
    verify(configEntryRepository, times(1)).save(any());
  }

  @Test
  void shouldUpdateExistingEntry() {
    ConfigEntry existing = new ConfigEntry();
    existing.setKey("key");
    existing.setValue("old");

    ConfigEntry entry = new ConfigEntry();
    entry.setConfigResourceId(1L);
    entry.setEnvironmentId(1L);
    entry.setKey("key");
    entry.setValue("new");

    when(configEntryRepository.findByConfigResourceIdAndEnvironmentIdAndKey(1L, 1L, "key"))
        .thenReturn(Optional.of(existing));
    when(configEntryRepository.save(any())).thenReturn(existing);

    ConfigEntry result = configEntryService.save(entry);

    assertThat(existing.getValue()).isEqualTo("new");
    verify(configEntryRepository, times(1)).save(existing);
  }

  @Test
  void shouldDeleteEntry() {
    configEntryService.delete(100L);
    verify(configEntryRepository, times(1)).deleteById(100L);
  }
}
