package io.vacivor.chert.server.infrastructure.persistence.config;

import io.vacivor.chert.server.domain.config.ConfigContent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigContentRepository extends JpaRepository<ConfigContent, Long> {

  Optional<ConfigContent> findFirstByConfigResourceIdAndEnvironmentIdOrderByCreatedAtDesc(
      Long configResourceId, Long environmentId);
}
