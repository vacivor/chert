package io.vacivor.chert.server.infrastructure.persistence.config;

import io.vacivor.chert.server.domain.config.ConfigReleaseRequest;
import io.vacivor.chert.server.domain.config.ConfigReleaseRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigReleaseRequestRepository extends JpaRepository<ConfigReleaseRequest, Long> {

  List<ConfigReleaseRequest> findByConfigResourceIdAndEnvironmentIdOrderByCreatedAtDesc(
      Long configResourceId,
      Long environmentId);

  Optional<ConfigReleaseRequest> findTopByConfigResourceIdAndEnvironmentIdAndStatusOrderByCreatedAtDesc(
      Long configResourceId,
      Long environmentId,
      ConfigReleaseRequestStatus status);
}
