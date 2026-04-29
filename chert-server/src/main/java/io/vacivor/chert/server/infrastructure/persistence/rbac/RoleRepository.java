package io.vacivor.chert.server.infrastructure.persistence.rbac;

import io.vacivor.chert.server.domain.rbac.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByCodeAndIsDeletedFalse(String code);

  boolean existsByCodeAndIsDeletedFalse(String code);

  List<Role> findByCodeInAndIsDeletedFalse(Collection<String> codes);
}
