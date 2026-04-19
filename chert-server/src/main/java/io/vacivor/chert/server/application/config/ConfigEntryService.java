package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigEntryRepository;
import java.util.List;
import java.util.Optional;
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
  public void delete(Long entryId) {
    configEntryRepository.deleteById(entryId);
  }
}
