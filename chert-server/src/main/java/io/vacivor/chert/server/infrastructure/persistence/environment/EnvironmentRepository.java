package io.vacivor.chert.server.infrastructure.persistence.environment;

import io.vacivor.chert.server.domain.environment.Environment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

  Optional<Environment> findByCodeAndIsDeletedFalse(String code);

}
