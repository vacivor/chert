package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.domain.config.ConfigReleaseHistory;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigReleaseHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfigReleaseHistoryService {

  private final ConfigReleaseHistoryRepository configReleaseHistoryRepository;

  public ConfigReleaseHistoryService(ConfigReleaseHistoryRepository configReleaseHistoryRepository) {
    this.configReleaseHistoryRepository = configReleaseHistoryRepository;
  }

  public List<ConfigReleaseHistory> list(Long resourceId, Long environmentId) {
    return configReleaseHistoryRepository.findByConfigResourceIdAndEnvironmentIdOrderByCreatedAtDesc(
        resourceId, environmentId);
  }
}
