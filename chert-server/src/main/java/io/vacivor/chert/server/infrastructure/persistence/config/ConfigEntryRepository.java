package io.vacivor.chert.server.infrastructure.persistence.config;

import io.vacivor.chert.server.domain.config.ConfigEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigEntryRepository extends JpaRepository<ConfigEntry, Long> {

  List<ConfigEntry> findByConfigResourceIdAndEnvironmentId(Long configResourceId, Long environmentId);

  Optional<ConfigEntry> findByConfigResourceIdAndEnvironmentIdAndKey(
      Long configResourceId, Long environmentId, String key);
}
