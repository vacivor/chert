package io.vacivor.chert.server.interfaces.admin.config;

import io.vacivor.chert.server.application.config.ConfigResourceService;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.error.ConfigResourceErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.interfaces.dto.config.ConfigResourceCreateRequest;
import io.vacivor.chert.server.interfaces.dto.config.ConfigResourceResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/applications/{appId}/config-resources")
public class ConfigResourceAdminController {

  private final ConfigResourceService configResourceService;

  public ConfigResourceAdminController(ConfigResourceService configResourceService) {
    this.configResourceService = configResourceService;
  }

  @PostMapping
  public ResponseEntity<ConfigResourceResponse> create(
      @PathVariable Long appId,
      @RequestBody ConfigResourceCreateRequest request) {
    if (request.configName() == null || request.configName().isBlank()) {
      throw new ValidationException(ConfigResourceErrorCode.CONFIG_RESOURCE_NAME_REQUIRED, "configName",
          "Config resource name cannot be blank");
    }
    if (request.type() == null) {
      throw new ValidationException(ConfigResourceErrorCode.CONFIG_RESOURCE_TYPE_REQUIRED, "type",
          "Config resource type is required");
    }
    if (request.format() == null) {
      throw new ValidationException(ConfigResourceErrorCode.CONFIG_RESOURCE_FORMAT_REQUIRED, "format",
          "Config resource format is required");
    }
    ConfigResource configResource = new ConfigResource();
    configResource.setApplicationId(appId);
    configResource.setName(request.configName());
    configResource.setType(request.type());
    configResource.setFormat(request.format());
    configResource.setVersion(request.version());
    configResource.setDescription(request.description());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ConfigResourceResponse.from(configResourceService.create(configResource)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ConfigResourceResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(ConfigResourceResponse.from(configResourceService.get(id)));
  }

  @GetMapping
  public ResponseEntity<List<ConfigResourceResponse>> list(@PathVariable Long appId) {
    return ResponseEntity.ok(configResourceService.listByApplication(appId).stream()
        .map(ConfigResourceResponse::from)
        .toList());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    configResourceService.delete(id);
    return ResponseEntity.noContent().build();
  }

}
