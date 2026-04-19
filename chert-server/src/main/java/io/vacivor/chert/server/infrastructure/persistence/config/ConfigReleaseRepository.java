package io.vacivor.chert.server.infrastructure.persistence.config;

import io.vacivor.chert.server.domain.config.ConfigRelease;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigReleaseRepository extends JpaRepository<ConfigRelease, Long> {

  Optional<ConfigRelease> findTopByConfigResourceIdAndEnvironmentIdOrderByVersionDesc(
      Long configResourceId, Long environmentId);
}
