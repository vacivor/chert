package io.vacivor.chert.server.infrastructure.persistence.config;

import io.vacivor.chert.server.domain.config.ConfigResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigResourceRepository extends JpaRepository<ConfigResource, Long> {

  List<ConfigResource> findByApplicationId(Long applicationId);

  Optional<ConfigResource> findByApplicationIdAndName(Long applicationId, String name);
}
