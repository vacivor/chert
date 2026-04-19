package io.vacivor.chert.server.interfaces.admin.app;

import io.vacivor.chert.server.application.app.ApplicationSecretService;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationSecretGenerateRequest;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationSecretResponse;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationSecretStatusUpdateRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console")
public class ApplicationSecretAdminController {

  private final ApplicationSecretService applicationSecretService;

  public ApplicationSecretAdminController(ApplicationSecretService applicationSecretService) {
    this.applicationSecretService = applicationSecretService;
  }

  @GetMapping("/applications/{appId}/secrets")
  public ResponseEntity<List<ApplicationSecretResponse>> listByAppId(@PathVariable Long appId) {
    return ResponseEntity.ok(
        applicationSecretService.listByAppId(appId).stream()
            .map(ApplicationSecretResponse::from)
            .toList());
  }

  @PostMapping("/applications/{appId}/secrets")
  public ResponseEntity<ApplicationSecretResponse> generate(
      @PathVariable Long appId,
      @RequestBody ApplicationSecretGenerateRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_SECRET_NAME_REQUIRED, "name", "Secret name cannot be blank");
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApplicationSecretResponse.from(applicationSecretService.generate(appId, request.name())));
  }

  @PatchMapping("/application-secrets/{id}/status")
  public ResponseEntity<ApplicationSecretResponse> updateStatus(
      @PathVariable Long id,
      @RequestBody ApplicationSecretStatusUpdateRequest request) {
    if (request.status() == null) {
      throw new ValidationException(CommonErrorCode.INVALID_PARAMETER, "status", "Status is required");
    }
    return ResponseEntity.ok(
        ApplicationSecretResponse.from(applicationSecretService.updateStatus(id, request.status())));
  }

  @DeleteMapping("/application-secrets/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    applicationSecretService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
