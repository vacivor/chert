package io.vacivor.chert.server.infrastructure.persistence.app;

import io.vacivor.chert.server.domain.app.ApplicationPublishPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationPublishPolicyRepository extends JpaRepository<ApplicationPublishPolicy, Long> {

  Optional<ApplicationPublishPolicy> findByApplicationIdAndEnvironmentId(Long applicationId, Long environmentId);
}
