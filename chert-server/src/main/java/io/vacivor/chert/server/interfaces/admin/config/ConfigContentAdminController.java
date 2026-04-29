package io.vacivor.chert.server.interfaces.admin.config;

import io.vacivor.chert.server.application.config.ConfigContentService;
import io.vacivor.chert.server.application.config.ConfigDiffService;
import io.vacivor.chert.server.domain.config.ConfigContent;
import io.vacivor.chert.server.error.ConfigContentErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.interfaces.dto.config.ConfigContentResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigContentSaveRequest;
import io.vacivor.chert.server.interfaces.dto.config.ConfigDiffResponse;
import io.vacivor.chert.server.security.ApplicationAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/config-resources/{resourceId}/environments/{environmentId}/contents")
public class ConfigContentAdminController {

  private final ConfigContentService configContentService;
  private final ConfigDiffService configDiffService;
  private final ApplicationAccessService applicationAccessService;

  public ConfigContentAdminController(
      ConfigContentService configContentService,
      ConfigDiffService configDiffService,
      ApplicationAccessService applicationAccessService) {
    this.configContentService = configContentService;
    this.configDiffService = configDiffService;
    this.applicationAccessService = applicationAccessService;
  }

  @GetMapping("/latest")
  public ResponseEntity<ConfigContentResponse> getLatest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    applicationAccessService.requireResourceMember(resourceId);
    return configContentService.findLatest(resourceId, environmentId)
        .map(ConfigContentResponse::from)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<ConfigContentResponse> save(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @RequestBody ConfigContentSaveRequest request) {
    applicationAccessService.requireResourceMember(resourceId);
    if (resourceId == null) {
      throw new ValidationException(ConfigContentErrorCode.CONFIG_CONTENT_RESOURCE_ID_REQUIRED, "configResourceId", "Resource id cannot be null");
    }
    if (environmentId == null) {
      throw new ValidationException(ConfigContentErrorCode.CONFIG_CONTENT_ENVIRONMENT_ID_REQUIRED, "environmentId", "Environment id cannot be null");
    }
    if (request.content() == null) {
      throw new ValidationException(ConfigContentErrorCode.CONFIG_CONTENT_REQUIRED, "content", "Content cannot be null");
    }
    ConfigContent configContent = new ConfigContent();
    configContent.setConfigResourceId(resourceId);
    configContent.setEnvironmentId(environmentId);
    configContent.setContent(request.content());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ConfigContentResponse.from(configContentService.save(configContent)));
  }

  @GetMapping("/diff")
  public ResponseEntity<ConfigDiffResponse> diff(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    applicationAccessService.requireResourceMember(resourceId);
    return configDiffService.diffDraftWithLatestRelease(resourceId, environmentId)
        .map(result -> new ConfigDiffResponse(
            result.oldContent(),
            result.newContent(),
            result.hasChanges()))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

}
