package io.vacivor.chert.server.interfaces.admin.app;

import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.security.ApplicationAccessService;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationCreateRequest;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationOwnerTransferRequest;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationResponse;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationUpdateRequest;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console")
public class ApplicationAdminController {

  private static final Pattern APP_ID_PATTERN = Pattern.compile("^[a-zA-Z_-][a-zA-Z0-9_-]*$");
  private final ApplicationService applicationService;
  private final ApplicationAccessService applicationAccessService;

  public ApplicationAdminController(
      ApplicationService applicationService,
      ApplicationAccessService applicationAccessService) {
    this.applicationService = applicationService;
    this.applicationAccessService = applicationAccessService;
  }

  @PostMapping("/applications")
  public ResponseEntity<ApplicationResponse> create(@RequestBody ApplicationCreateRequest request) {
    if (request.appId() == null || request.appId().isBlank()) {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_APP_ID_REQUIRED, "appId", "Application appId cannot be blank");
    }
    if (!APP_ID_PATTERN.matcher(request.appId()).matches()) {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_INVALID_APP_ID, "appId",
          "Invalid application appId: " + request.appId() + ". appId must start with a letter and contain only letters, numbers, underscores, and hyphens.");
    }
    if (request.name() == null || request.name().isBlank()) {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_NAME_REQUIRED, "name", "Application name cannot be blank");
    }
    if (request.ownerUserId() == null) {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_OWNER_TRANSFER_REQUIRED, "ownerUserId", "Application owner cannot be blank");
    }
    if (request.maintainerUserId() == null) {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_MAINTAINER_REQUIRED, "maintainerUserId", "Application maintainer cannot be blank");
    }
    Application application = new Application();
    application.setAppId(request.appId());
    application.setName(request.name());
    application.setDescription(request.description());
    application.setOwner(applicationService.resolveUser(request.ownerUserId(), "ownerUserId"));
    application.setMaintainer(applicationService.resolveUser(request.maintainerUserId(), "maintainerUserId"));
    application.setDevelopers(applicationService.resolveUsers(request.developerUserIds()));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApplicationResponse.from(applicationService.create(application)));
  }

  @GetMapping("/applications/{id}")
  public ResponseEntity<ApplicationResponse> get(@PathVariable Long id) {
    applicationAccessService.requireApplicationMember(id);
    return ResponseEntity.ok(ApplicationResponse.from(applicationService.get(id)));
  }

  @GetMapping("/applications")
  public ResponseEntity<Page<ApplicationResponse>> list(Pageable pageable) {
    Page<Application> applications = applicationAccessService.isSuperAdmin()
        ? applicationService.list(pageable)
        : applicationService.listAccessible(applicationAccessService.currentUserId(), pageable);
    return ResponseEntity.ok(applications.map(ApplicationResponse::from));
  }

  @PatchMapping("/applications/{id}")
  public ResponseEntity<ApplicationResponse> update(
      @PathVariable Long id,
      @RequestBody ApplicationUpdateRequest request) {
    applicationAccessService.requireApplicationManager(id);
    if (request.name() != null && request.name().isBlank()) {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_NAME_REQUIRED, "name", "Application name cannot be blank");
    }
    if (request.ownerUserId() != null) {
      throw new ValidationException(
          ApplicationErrorCode.APPLICATION_OWNER_UPDATE_FORBIDDEN,
          "ownerUserId",
          "Application owner must be changed through owner transfer");
    }
    Application application = new Application();
    application.setName(request.name());
    application.setDescription(request.description());
    if (request.maintainerUserId() != null) {
      application.setMaintainer(applicationService.resolveUser(request.maintainerUserId(), "maintainerUserId"));
    }
    if (request.developerUserIds() != null) {
      application.setDevelopers(applicationService.resolveUsers(request.developerUserIds()));
    }
    return ResponseEntity.ok(ApplicationResponse.from(applicationService.update(id, application)));
  }

  @PostMapping("/applications/{id}/owner/transfer")
  public ResponseEntity<ApplicationResponse> transferOwner(
      @PathVariable Long id,
      @RequestBody ApplicationOwnerTransferRequest request) {
    applicationAccessService.requireApplicationOwner(id);
    if (request.ownerUserId() == null) {
      throw new ValidationException(
          ApplicationErrorCode.APPLICATION_OWNER_TRANSFER_REQUIRED,
          "ownerUserId",
          "Application owner transfer target cannot be blank");
    }
    return ResponseEntity.ok(ApplicationResponse.from(
        applicationService.transferOwner(
            id,
            applicationService.resolveUser(request.ownerUserId(), "ownerUserId"))));
  }

  @DeleteMapping("/applications/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    applicationAccessService.requireApplicationManager(id);
    applicationService.delete(id);
    return ResponseEntity.noContent().build();
  }

}
