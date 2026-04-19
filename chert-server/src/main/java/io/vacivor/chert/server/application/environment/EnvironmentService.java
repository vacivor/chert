package io.vacivor.chert.server.application.environment;

import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.EnvironmentErrorCode;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.infrastructure.persistence.environment.EnvironmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EnvironmentService {

  private final EnvironmentRepository environmentRepository;
  private final AuditLogService auditLogService;

  public EnvironmentService(EnvironmentRepository environmentRepository, AuditLogService auditLogService) {
    this.environmentRepository = environmentRepository;
    this.auditLogService = auditLogService;
  }

  @Transactional
  public Environment create(Environment environment) {
    environmentRepository.findByCodeAndIsDeletedFalse(environment.getCode())
        .ifPresent(existing -> {
          throw new ConflictException(EnvironmentErrorCode.ENVIRONMENT_CODE_DUPLICATED,
              "Environment code '" + environment.getCode() + "' already exists");
        });
    environment.setUpdatedAt(Instant.now());
    environment.setDeleted(false);
    Environment saved = environmentRepository.save(environment);
    auditLogService.log(null, "ENVIRONMENT", "CREATE", 
        saved.getId() != null ? saved.getId().toString() : null, "Code: " + saved.getCode());
    return saved;
  }

  public Environment get(Long id) {
    return environmentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(EnvironmentErrorCode.ENVIRONMENT_NOT_FOUND,
            "Environment not found: " + id));
  }

  public List<Environment> list() {
    return environmentRepository.findAll();
  }

  @Transactional
  public Environment update(Long id, Environment updated) {
    Environment environment = get(id);
    if (updated.getCode() != null && !updated.getCode().equals(environment.getCode())) {
      environmentRepository.findByCodeAndIsDeletedFalse(updated.getCode())
          .ifPresent(existing -> {
            throw new ConflictException(EnvironmentErrorCode.ENVIRONMENT_CODE_DUPLICATED,
                "Environment code '" + updated.getCode() + "' already exists");
          });
    }
    if (updated.getName() != null) {
      environment.setName(updated.getName());
    }
    if (updated.getCode() != null) {
      environment.setCode(updated.getCode());
    }
    if (updated.getDescription() != null) {
      environment.setDescription(updated.getDescription());
    }
    environment.setUpdatedAt(Instant.now());
    Environment saved = environmentRepository.save(environment);
    auditLogService.log(null, "ENVIRONMENT", "UPDATE", 
        saved.getId() != null ? saved.getId().toString() : null, "Updated name or code");
    return saved;
  }

  @Transactional
  public void delete(Long id) {
    if (!environmentRepository.existsById(id)) {
      throw new NotFoundException(EnvironmentErrorCode.ENVIRONMENT_NOT_FOUND, "Environment not found: " + id);
    }
    environmentRepository.deleteById(id);
    auditLogService.log(null, "ENVIRONMENT", "DELETE", id.toString(), null);
  }
}
