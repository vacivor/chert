package io.vacivor.chert.server.infrastructure.persistence.rbac;

import io.vacivor.chert.server.domain.rbac.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByCodeAndIsDeletedFalse(String code);

  boolean existsByCodeAndIsDeletedFalse(String code);

  List<Permission> findByCodeInAndIsDeletedFalse(Collection<String> codes);
}
