package io.vacivor.chert.server.infrastructure.persistence.user;

import io.vacivor.chert.server.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<User, Long> {

  @EntityGraph(attributePaths = {"roles", "roles.permissions"})
  Optional<User> findByUsernameAndIsDeletedFalse(String username);

  boolean existsByUsernameAndIsDeletedFalse(String username);

  boolean existsByEmailAndIsDeletedFalse(String email);
}
