package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.domain.config.ConfigContent;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigContentRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfigContentService {

  private final ConfigContentRepository configContentRepository;

  public ConfigContentService(ConfigContentRepository configContentRepository) {
    this.configContentRepository = configContentRepository;
  }

  public Optional<ConfigContent> findLatest(Long resourceId, Long environmentId) {
    return configContentRepository.findFirstByConfigResourceIdAndEnvironmentIdOrderByCreatedAtDesc(
        resourceId, environmentId);
  }

  @Transactional
  public ConfigContent save(ConfigContent configContent) {
    configContent.setCreatedAt(Instant.now());
    configContent.setUpdatedAt(Instant.now());
    configContent.setDeleted(false);
    return configContentRepository.save(configContent);
  }
}
