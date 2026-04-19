package io.vacivor.chert.server.interfaces.admin.environment;

import io.vacivor.chert.server.application.environment.EnvironmentService;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.error.EnvironmentErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.interfaces.dto.environment.EnvironmentCreateRequest;
import io.vacivor.chert.server.interfaces.dto.environment.EnvironmentResponse;
import io.vacivor.chert.server.interfaces.dto.environment.EnvironmentUpdateRequest;
import java.util.List;
import java.util.regex.Pattern;
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
public class EnvironmentAdminController {

  private static final Pattern CODE_PATTERN = Pattern.compile("^[a-zA-Z_-][a-zA-Z0-9_-]*$");
  private final EnvironmentService environmentService;

  public EnvironmentAdminController(EnvironmentService environmentService) {
    this.environmentService = environmentService;
  }

  @PostMapping("/environments")
  public ResponseEntity<EnvironmentResponse> create(@RequestBody EnvironmentCreateRequest request) {
    if (request.code() == null || request.code().isBlank()) {
      throw new ValidationException(EnvironmentErrorCode.ENVIRONMENT_CODE_REQUIRED, "code",
          "Environment code cannot be blank");
    }
    if (!CODE_PATTERN.matcher(request.code()).matches()) {
      throw new ValidationException(EnvironmentErrorCode.ENVIRONMENT_INVALID_CODE, "code",
          "Invalid environment code: " + request.code());
    }
    if (request.name() == null || request.name().isBlank()) {
      throw new ValidationException(EnvironmentErrorCode.ENVIRONMENT_NAME_REQUIRED, "name",
          "Environment name cannot be blank");
    }
    Environment environment = new Environment();
    environment.setCode(request.code());
    environment.setName(request.name());
    environment.setDescription(request.description());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(EnvironmentResponse.from(environmentService.create(environment)));
  }

  @GetMapping("/environments")
  public ResponseEntity<List<EnvironmentResponse>> list() {
    return ResponseEntity.ok(environmentService.list().stream()
        .map(EnvironmentResponse::from)
        .toList());
  }

  @GetMapping("/environments/{id}")
  public ResponseEntity<EnvironmentResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(EnvironmentResponse.from(environmentService.get(id)));
  }

  @DeleteMapping("/environments/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    environmentService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/environments/{id}")
  public ResponseEntity<EnvironmentResponse> update(
      @PathVariable Long id,
      @RequestBody EnvironmentUpdateRequest request) {
    if (request.code() != null) {
      if (request.code().isBlank()) {
        throw new ValidationException(EnvironmentErrorCode.ENVIRONMENT_CODE_REQUIRED, "code",
            "Environment code cannot be blank");
      }
      if (!CODE_PATTERN.matcher(request.code()).matches()) {
        throw new ValidationException(EnvironmentErrorCode.ENVIRONMENT_INVALID_CODE, "code",
            "Invalid environment code: " + request.code());
      }
    }
    if (request.name() != null && request.name().isBlank()) {
      throw new ValidationException(EnvironmentErrorCode.ENVIRONMENT_NAME_REQUIRED, "name",
          "Environment name cannot be blank");
    }
    Environment environment = new Environment();
    environment.setCode(request.code());
    environment.setName(request.name());
    environment.setDescription(request.description());
    return ResponseEntity.ok(EnvironmentResponse.from(environmentService.update(id, environment)));
  }
}
