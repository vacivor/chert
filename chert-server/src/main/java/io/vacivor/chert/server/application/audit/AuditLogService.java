package io.vacivor.chert.server.application.audit;

import io.vacivor.chert.server.domain.audit.AuditLog;
import io.vacivor.chert.server.infrastructure.persistence.audit.AuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(String operator, String domain, String action, String targetId, String details) {
    AuditLog log = new AuditLog();
    log.setOperator(operator != null ? operator : "SYSTEM");
    log.setDomain(domain);
    log.setAction(action);
    log.setTargetId(targetId);
    log.setDetails(details);
    log.setCreatedAt(Instant.now());
    auditLogRepository.save(log);
  }
}
