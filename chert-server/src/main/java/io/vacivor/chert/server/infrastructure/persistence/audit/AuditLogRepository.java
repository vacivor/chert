package io.vacivor.chert.server.infrastructure.persistence.audit;

import io.vacivor.chert.server.domain.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
