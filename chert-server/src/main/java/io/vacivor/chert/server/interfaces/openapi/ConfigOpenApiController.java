package io.vacivor.chert.server.interfaces.openapi;

import io.vacivor.chert.client.ChertConfigResponse;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.application.app.ApplicationSecretService;
import io.vacivor.chert.server.application.config.ConfigEntryService;
import io.vacivor.chert.server.application.config.ConfigReleaseService;
import io.vacivor.chert.server.application.config.ConfigResourceService;
import io.vacivor.chert.server.application.environment.EnvironmentService;
import io.vacivor.chert.server.common.ConfigFormat;
import io.vacivor.chert.server.common.ConfigType;
import io.vacivor.chert.server.config.ChertServerProperties;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.app.ApplicationSecret;
import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.domain.config.ConfigRelease;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.ConfigReleaseErrorCode;
import io.vacivor.chert.server.error.EnvironmentErrorCode;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.UnauthorizedException;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.error.ConfigResourceErrorCode;
import io.vacivor.chert.server.infrastructure.persistence.config.ReleaseMessageRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/api/open/configs")
public class ConfigOpenApiController {

  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;
  private final ConfigResourceService configResourceService;
  private final ConfigReleaseService configReleaseService;
  private final ConfigEntryService configEntryService;
  private final ApplicationSecretService applicationSecretService;

  public ConfigOpenApiController(
      ApplicationService applicationService,
      EnvironmentService environmentService,
      ConfigResourceService configResourceService,
      ConfigReleaseService configReleaseService,
      ConfigEntryService configEntryService,
      ApplicationSecretService applicationSecretService) {
    this.applicationService = applicationService;
    this.environmentService = environmentService;
    this.configResourceService = configResourceService;
    this.configReleaseService = configReleaseService;
    this.configEntryService = configEntryService;
    this.applicationSecretService = applicationSecretService;
  }

  @GetMapping("/{appId}/{env}/{configName}")
  public ResponseEntity<ChertConfigResponse> fetchConfig(
      @PathVariable String appId,
      @PathVariable String env,
      @PathVariable String configName,
      HttpServletRequest request) {
    Application application = applicationService.getByAppId(appId);
    checkAuth(application, request);

    Environment environment = findEnvironmentByCode(env);
    ConfigResource resource = configResourceService.getByApplicationAndName(application.getId(), configName);

    ConfigRelease release = configReleaseService.findLatest(resource.getId(), environment.getId())
        .orElseThrow(() -> new NotFoundException(ConfigReleaseErrorCode.CONFIG_RELEASE_NOT_FOUND,
            "No release found for " + appId + "/" + env + "/" + configName));

    return ResponseEntity.ok(new ChertConfigResponse(
        release.getSnapshot(),
        release.getUpdatedAt(),
        io.vacivor.chert.client.ConfigType.valueOf(resource.getType().name()),
        io.vacivor.chert.client.ConfigFormat.valueOf(resource.getFormat().name())
    ));
  }

  @Transactional
  @PostMapping("/{appId}/{env}/{configName}")
  public ResponseEntity<Void> updateConfig(
      @PathVariable String appId,
      @PathVariable String env,
      @PathVariable String configName,
      @RequestBody java.util.Map<String, String> entries,
      HttpServletRequest request) {
    Application application = applicationService.getByAppId(appId);
    checkAuth(application, request);

    Environment environment = findEnvironmentByCode(env);
    ConfigResource resource = configResourceService.getByApplicationAndName(application.getId(), configName);

    if (resource.getType() != ConfigType.ENTRIES) {
      throw new ValidationException(ConfigResourceErrorCode.CONFIG_RESOURCE_TYPE_NOT_SUPPORTED,
          "type", "Only ENTRIES type configuration can be updated via client");
    }

    if (entries == null || entries.isEmpty()) {
      return ResponseEntity.ok().build();
    }

    // Save or Update entries
    for (java.util.Map.Entry<String, String> entry : entries.entrySet()) {
      if (entry.getKey() == null || entry.getKey().isBlank()) {
        continue; // Or throw ValidationException if we want to be strict
      }
      ConfigEntry configEntry = new ConfigEntry();
      configEntry.setConfigResourceId(resource.getId());
      configEntry.setEnvironmentId(environment.getId());
      configEntry.setKey(entry.getKey());
      configEntry.setValue(entry.getValue());
      configEntry.setValueType("STRING"); // Default for client updates
      configEntryService.save(configEntry);
    }

    // Publish new release
    configReleaseService.publish(resource.getId(), environment.getId(), "Updated by client via OpenAPI");

    return ResponseEntity.ok().build();
  }


  private void checkAuth(Application application, HttpServletRequest request) {
    String accessKey = request.getHeader("X-Chert-Access-Key");
    String secretKey = request.getHeader("X-Chert-Secret-Key");
    if (accessKey == null || accessKey.isEmpty()) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "X-Chert-Access-Key is missing");
    }
    if (secretKey == null || secretKey.isEmpty()) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "X-Chert-Secret-Key is missing");
    }
    ApplicationSecret secret = applicationSecretService.findActiveByAccessKey(accessKey);
    if (!secret.getSecretKey().equals(secretKey)) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "SecretKey does not match");
    }
    if (!secret.getAppId().equals(application.getId())) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "AccessKey does not match appId");
    }
    applicationSecretService.touch(secret.getId());
  }

  private Environment findEnvironmentByCode(String code) {
    return environmentService.list().stream()
        .filter(e -> e.getCode().equals(code))
        .findFirst()
        .orElseThrow(() -> new NotFoundException(EnvironmentErrorCode.ENVIRONMENT_NOT_FOUND, "Environment not found: " + code));
  }

}
