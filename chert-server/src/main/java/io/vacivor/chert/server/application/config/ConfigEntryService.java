package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigEntryRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfigEntryService {

  private final ConfigEntryRepository configEntryRepository;

  public ConfigEntryService(ConfigEntryRepository configEntryRepository) {
    this.configEntryRepository = configEntryRepository;
  }

  public List<ConfigEntry> list(Long resourceId, Long environmentId) {
    return configEntryRepository.findByConfigResourceIdAndEnvironmentId(resourceId, environmentId);
  }

  @Transactional
  public ConfigEntry save(ConfigEntry entry) {
    Optional<ConfigEntry> existingOpt = configEntryRepository
        .findByConfigResourceIdAndEnvironmentIdAndKey(
            entry.getConfigResourceId(), entry.getEnvironmentId(), entry.getKey());

    if (existingOpt.isPresent()) {
      ConfigEntry existing = existingOpt.get();
      existing.setValue(entry.getValue());
      existing.setValueType(entry.getValueType());
      existing.setDescription(entry.getDescription());
      return configEntryRepository.save(existing);
    }
    return configEntryRepository.save(entry);
  }

  @Transactional
  public void replaceEntries(Long resourceId, Long environmentId, Map<String, String> entries) {
    List<ConfigEntry> existingEntries =
        configEntryRepository.findByConfigResourceIdAndEnvironmentId(resourceId, environmentId);
    Set<String> incomingKeys = new HashSet<>();

    for (Map.Entry<String, String> entry : entries.entrySet()) {
      if (entry.getKey() == null || entry.getKey().isBlank()) {
        continue;
      }
      incomingKeys.add(entry.getKey());

      ConfigEntry configEntry = new ConfigEntry();
      configEntry.setConfigResourceId(resourceId);
      configEntry.setEnvironmentId(environmentId);
      configEntry.setKey(entry.getKey());
      configEntry.setValue(entry.getValue());
      configEntry.setValueType("STRING");
      save(configEntry);
    }

    List<ConfigEntry> staleEntries = existingEntries.stream()
        .filter(existing -> !incomingKeys.contains(existing.getKey()))
        .toList();
    if (!staleEntries.isEmpty()) {
      configEntryRepository.deleteAll(staleEntries);
    }
  }

  @Transactional
  public void delete(Long entryId) {
    configEntryRepository.deleteById(entryId);
  }
}
