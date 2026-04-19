package io.vacivor.chert.server.application.app;

import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.infrastructure.persistence.app.ApplicationRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final AuditLogService auditLogService;

  public ApplicationService(ApplicationRepository applicationRepository, AuditLogService auditLogService) {
    this.applicationRepository = applicationRepository;
    this.auditLogService = auditLogService;
  }

  @Transactional
  public Application create(Application application) {
    applicationRepository.findByAppIdAndIsDeletedFalse(application.getAppId())
        .ifPresent(existing -> {
          throw new ConflictException(ApplicationErrorCode.APPLICATION_APP_ID_DUPLICATED,
              "Application with appId '" + application.getAppId() + "' already exists");
        });
    application.setUpdatedAt(Instant.now());
    application.setDeleted(false);
    Application saved = applicationRepository.save(application);
    auditLogService.log(null, "APPLICATION", "CREATE", 
        saved.getId() != null ? saved.getId().toString() : null, "AppId: " + saved.getAppId());
    return saved;
  }

  public Application get(Long id) {
    return applicationRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(ApplicationErrorCode.APPLICATION_NOT_FOUND,
            "Application not found: " + id));
  }

  public Application getByAppId(String appId) {
    return applicationRepository.findByAppIdAndIsDeletedFalse(appId)
        .orElseThrow(() -> new NotFoundException(ApplicationErrorCode.APPLICATION_NOT_FOUND,
            "Application not found: " + appId));
  }

  public Page<Application> list(Pageable pageable) {
    return applicationRepository.findAll(pageable);
  }

  @Transactional
  public Application update(Long id, Application updated) {
    Application application = get(id);
    if (updated.getName() != null) {
      application.setName(updated.getName());
    }
    if (updated.getDescription() != null) {
      application.setDescription(updated.getDescription());
    }
    application.setUpdatedAt(Instant.now());
    Application saved = applicationRepository.save(application);
    auditLogService.log(null, "APPLICATION", "UPDATE", 
        saved.getId() != null ? saved.getId().toString() : null, "Updated name or description");
    return saved;
  }

  @Transactional
  public void delete(Long id) {
    if (!applicationRepository.existsById(id)) {
      throw new NotFoundException(ApplicationErrorCode.APPLICATION_NOT_FOUND, "Application not found: " + id);
    }
    applicationRepository.deleteById(id);
    auditLogService.log(null, "APPLICATION", "DELETE", id.toString(), null);
  }
}
