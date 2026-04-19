package io.vacivor.chert.server.infrastructure.persistence.app;

import io.vacivor.chert.server.domain.app.Application;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  Optional<Application> findByAppIdAndIsDeletedFalse(String appId);
}
