package io.vacivor.chert.server.infrastructure.persistence.config;

import io.vacivor.chert.server.domain.config.ConfigReleaseHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigReleaseHistoryRepository extends JpaRepository<ConfigReleaseHistory, Long> {

  List<ConfigReleaseHistory> findByConfigResourceIdAndEnvironmentIdOrderByCreatedAtDesc(
      Long configResourceId, Long environmentId);
}
