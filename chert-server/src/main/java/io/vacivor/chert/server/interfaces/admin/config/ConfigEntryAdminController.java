package io.vacivor.chert.server.interfaces.admin.config;

import io.vacivor.chert.server.application.config.ConfigEntryService;
import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.error.ConfigEntryErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.interfaces.dto.config.ConfigEntryRequest;
import io.vacivor.chert.server.interfaces.dto.config.ConfigEntryResponse;
import io.vacivor.chert.server.security.ApplicationAccessService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/config-resources/{resourceId}/environments/{environmentId}/entries")
public class ConfigEntryAdminController {

  private final ConfigEntryService configEntryService;
  private final ApplicationAccessService applicationAccessService;

  public ConfigEntryAdminController(
      ConfigEntryService configEntryService,
      ApplicationAccessService applicationAccessService) {
    this.configEntryService = configEntryService;
    this.applicationAccessService = applicationAccessService;
  }

  @GetMapping
  public ResponseEntity<List<ConfigEntryResponse>> list(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    applicationAccessService.requireResourceMember(resourceId);
    return ResponseEntity.ok(configEntryService.list(resourceId, environmentId).stream()
        .map(ConfigEntryResponse::from)
        .toList());
  }

  @PostMapping
  public ResponseEntity<ConfigEntryResponse> save(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @RequestBody ConfigEntryRequest request) {
    applicationAccessService.requireResourceMember(resourceId);
    if (resourceId == null) {
      throw new ValidationException(ConfigEntryErrorCode.CONFIG_ENTRY_RESOURCE_ID_REQUIRED, "configResourceId", "Resource id cannot be null");
    }
    if (environmentId == null) {
      throw new ValidationException(ConfigEntryErrorCode.CONFIG_ENTRY_ENVIRONMENT_ID_REQUIRED, "environmentId", "Environment id cannot be null");
    }
    if (request.key() == null || request.key().isBlank()) {
      throw new ValidationException(ConfigEntryErrorCode.CONFIG_ENTRY_KEY_REQUIRED, "key", "Key cannot be blank");
    }
    if (request.value() == null) {
      throw new ValidationException(ConfigEntryErrorCode.CONFIG_ENTRY_VALUE_REQUIRED, "value", "Value cannot be null");
    }
    ConfigEntry entry = new ConfigEntry();
    entry.setConfigResourceId(resourceId);
    entry.setEnvironmentId(environmentId);
    entry.setKey(request.key());
    entry.setValue(request.value());
    entry.setValueType(request.valueType());
    entry.setDescription(request.description());
    return ResponseEntity.ok(ConfigEntryResponse.from(configEntryService.save(entry)));
  }

  @DeleteMapping("/{entryId}")
  public ResponseEntity<Void> delete(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @PathVariable Long entryId) {
    applicationAccessService.requireResourceMember(resourceId);
    configEntryService.delete(entryId);
    return ResponseEntity.noContent().build();
  }
}
