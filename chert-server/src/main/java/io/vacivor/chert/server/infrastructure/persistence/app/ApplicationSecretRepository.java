package io.vacivor.chert.server.infrastructure.persistence.app;

import io.vacivor.chert.server.domain.app.ApplicationSecret;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSecretRepository extends JpaRepository<ApplicationSecret, Long> {

  List<ApplicationSecret> findAllByAppId(Long appId);

  Optional<ApplicationSecret> findByAccessKey(String accessKey);
}
