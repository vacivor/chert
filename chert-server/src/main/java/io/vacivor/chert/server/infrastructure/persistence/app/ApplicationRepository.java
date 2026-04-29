package io.vacivor.chert.server.infrastructure.persistence.app;

import io.vacivor.chert.server.domain.app.Application;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  @Override
  @EntityGraph(attributePaths = {"owner", "maintainer", "developers"})
  Optional<Application> findById(Long id);

  @Override
  @EntityGraph(attributePaths = {"owner", "maintainer", "developers"})
  Page<Application> findAll(Pageable pageable);

  @EntityGraph(attributePaths = {"owner", "maintainer", "developers"})
  Optional<Application> findByAppIdAndIsDeletedFalse(String appId);

  @EntityGraph(attributePaths = {"owner", "maintainer", "developers"})
  Page<Application> findDistinctByOwnerIdOrMaintainerIdOrDevelopers_Id(
      Long ownerId,
      Long maintainerId,
      Long developerId,
      Pageable pageable);
}
