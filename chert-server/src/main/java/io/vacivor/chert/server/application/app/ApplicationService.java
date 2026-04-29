package io.vacivor.chert.server.application.app;

import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.user.User;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.infrastructure.persistence.app.ApplicationRepository;
import io.vacivor.chert.server.application.user.UserService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final AuditLogService auditLogService;
  private final UserService userService;

  public ApplicationService(
      ApplicationRepository applicationRepository,
      AuditLogService auditLogService,
      UserService userService) {
    this.applicationRepository = applicationRepository;
    this.auditLogService = auditLogService;
    this.userService = userService;
  }

  @Transactional
  public Application create(Application application) {
    applicationRepository.findByAppIdAndIsDeletedFalse(application.getAppId())
        .ifPresent(existing -> {
          throw new ConflictException(ApplicationErrorCode.APPLICATION_APP_ID_DUPLICATED,
              "Application with appId '" + application.getAppId() + "' already exists");
        });
    validateMembers(application.getOwner(), application.getMaintainer(), application.getDevelopers());
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

  public Page<Application> listAccessible(Long userId, Pageable pageable) {
    return applicationRepository.findDistinctByOwnerIdOrMaintainerIdOrDevelopers_Id(
        userId, userId, userId, pageable);
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
    if (updated.getMaintainer() != null) {
      application.setMaintainer(updated.getMaintainer());
    }
    if (updated.getDevelopers() != null) {
      application.setDevelopers(updated.getDevelopers());
    }
    validateMembers(application.getOwner(), application.getMaintainer(), application.getDevelopers());
    Application saved = applicationRepository.save(application);
    auditLogService.log(null, "APPLICATION", "UPDATE", 
        saved.getId() != null ? saved.getId().toString() : null, "Updated name or description");
    return saved;
  }

  @Transactional
  public Application transferOwner(Long id, User newOwner) {
    Application application = get(id);
    if (newOwner == null) {
      throw new ValidationException(
          ApplicationErrorCode.APPLICATION_OWNER_TRANSFER_REQUIRED,
          "ownerUserId",
          "Application owner transfer target cannot be null");
    }
    application.setOwner(newOwner);
    if (application.getDevelopers() != null) {
      application.getDevelopers().removeIf(developer -> developer.getId().equals(newOwner.getId()));
    }
    validateMembers(application.getOwner(), application.getMaintainer(), application.getDevelopers());
    Application saved = applicationRepository.save(application);
    auditLogService.log(
        null,
        "APPLICATION",
        "TRANSFER_OWNER",
        saved.getId() != null ? saved.getId().toString() : null,
        "Transferred owner to userId=" + newOwner.getId());
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

  public User resolveUser(Long userId, String field) {
    if (userId == null) {
      throw new ValidationException(CommonErrorCode.INVALID_PARAMETER, field, field + " cannot be null");
    }
    return userService.get(userId);
  }

  public Set<User> resolveUsers(Set<Long> userIds) {
    Set<User> users = new LinkedHashSet<>();
    if (userIds == null) {
      return users;
    }
    for (Long userId : userIds) {
      if (userId != null) {
        users.add(userService.get(userId));
      }
    }
    return users;
  }

  private void validateMembers(User owner, User maintainer, Set<User> developers) {
    if (owner == null) {
      throw new ValidationException(
          ApplicationErrorCode.APPLICATION_OWNER_TRANSFER_REQUIRED,
          "ownerUserId",
          "Application owner is required");
    }
    if (maintainer == null) {
      throw new ValidationException(
          ApplicationErrorCode.APPLICATION_MAINTAINER_REQUIRED,
          "maintainerUserId",
          "Application maintainer is required");
    }
    if (developers == null || developers.isEmpty()) {
      return;
    }
    for (User developer : developers) {
      if (developer == null || developer.getId() == null) {
        continue;
      }
      if (developer.getId().equals(owner.getId())) {
        throw new ValidationException(
            ApplicationErrorCode.APPLICATION_DEVELOPER_CONFLICT,
            "developerUserIds",
            "Application owner cannot also be configured as a developer");
      }
      if (developer.getId().equals(maintainer.getId())) {
        throw new ValidationException(
            ApplicationErrorCode.APPLICATION_DEVELOPER_CONFLICT,
            "developerUserIds",
            "Application maintainer cannot also be configured as a developer");
      }
    }
  }
}
