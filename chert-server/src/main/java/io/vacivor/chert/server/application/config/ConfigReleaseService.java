package io.vacivor.chert.server.application.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.application.environment.EnvironmentService;
import io.vacivor.chert.server.common.ConfigType;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.config.ConfigContent;
import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.domain.config.ConfigRelease;
import io.vacivor.chert.server.domain.config.ConfigReleaseHistory;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.domain.config.ReleaseMessage;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.ConfigReleaseErrorCode;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigReleaseHistoryRepository;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigReleaseRepository;
import io.vacivor.chert.server.infrastructure.persistence.config.ReleaseMessageRepository;
import io.vacivor.chert.server.interfaces.openapi.ConfigNotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class ConfigReleaseService {

  private final ConfigReleaseRepository configReleaseRepository;
  private final ConfigReleaseHistoryRepository configReleaseHistoryRepository;
  private final ReleaseMessageRepository releaseMessageRepository;
  private final ConfigContentService configContentService;
  private final ConfigEntryService configEntryService;
  private final ConfigResourceService configResourceService;
  private final AuditLogService auditLogService;
  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;
  private final ConfigNotificationService configNotificationService;
  private final ObjectMapper objectMapper;

  public ConfigReleaseService(
      ConfigReleaseRepository configReleaseRepository,
      ConfigReleaseHistoryRepository configReleaseHistoryRepository,
      ReleaseMessageRepository releaseMessageRepository,
      ConfigContentService configContentService,
      ConfigEntryService configEntryService,
      ConfigResourceService configResourceService,
      AuditLogService auditLogService,
      ApplicationService applicationService,
      EnvironmentService environmentService,
      ConfigNotificationService configNotificationService,
      ObjectMapper objectMapper) {
    this.configReleaseRepository = configReleaseRepository;
    this.configReleaseHistoryRepository = configReleaseHistoryRepository;
    this.releaseMessageRepository = releaseMessageRepository;
    this.configContentService = configContentService;
    this.configEntryService = configEntryService;
    this.configResourceService = configResourceService;
    this.auditLogService = auditLogService;
    this.applicationService = applicationService;
    this.environmentService = environmentService;
    this.configNotificationService = configNotificationService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public ConfigRelease publish(Long resourceId, Long environmentId, String comment) {
    ConfigResource resource = configResourceService.get(resourceId);
    String snapshot = captureSnapshot(resource, environmentId);

    Optional<ConfigRelease> latestReleaseOpt = configReleaseRepository
        .findTopByConfigResourceIdAndEnvironmentIdOrderByVersionDesc(resourceId, environmentId);

    long version = 1L;
    Long previousReleaseId = null;
    if (latestReleaseOpt.isPresent()) {
      ConfigRelease latestRelease = latestReleaseOpt.get();
      if (latestRelease.getSnapshot().equals(snapshot)) {
        return latestRelease; // 内容没变，直接返回
      }
      version = latestRelease.getVersion() + 1;
      previousReleaseId = latestRelease.getId();
    }

    ConfigRelease release = new ConfigRelease();
    release.setConfigResourceId(resourceId);
    release.setEnvironmentId(environmentId);
    release.setSnapshot(snapshot);
    release.setVersion(version);
    release.setType("NORMAL");
    release.setComment(comment);
    release.setCreatedAt(Instant.now());
    release.setUpdatedAt(Instant.now());
    release.setDeleted(false);

    ConfigRelease savedRelease = configReleaseRepository.save(release);

    ConfigReleaseHistory history = new ConfigReleaseHistory();
    history.setConfigResourceId(resourceId);
    history.setEnvironmentId(environmentId);
    history.setReleaseId(savedRelease.getId());
    history.setPreviousReleaseId(previousReleaseId);
    history.setCreatedAt(Instant.now());
    configReleaseHistoryRepository.save(history);

    saveReleaseMessage(savedRelease);
    notifyWatchers(resourceId, environmentId);

    auditLogService.log(null, "CONFIG_RELEASE", "PUBLISH", savedRelease.getId().toString(),
        "Resource: " + resourceId + ", Env: " + environmentId + ", Version: " + savedRelease.getVersion());

    return savedRelease;
  }

  private String captureSnapshot(ConfigResource resource, Long environmentId) {
    if (resource.getType() == ConfigType.CONTENT) {
      return configContentService.findLatest(resource.getId(), environmentId)
          .map(ConfigContent::getContent)
          .orElseThrow(() -> new NotFoundException(ConfigReleaseErrorCode.CONFIG_RELEASE_TARGET_NOT_FOUND,
              "No content found for resource " + resource.getId() + " in environment " + environmentId));
    } else if (resource.getType() == ConfigType.ENTRIES) {
      List<ConfigEntry> entries = configEntryService.list(resource.getId(), environmentId);
      if (entries.isEmpty()) {
        throw new NotFoundException(ConfigReleaseErrorCode.CONFIG_RELEASE_TARGET_NOT_FOUND,
            "No entries found for resource " + resource.getId() + " in environment " + environmentId);
      }
      try {
        return objectMapper.writeValueAsString(entries);
      } catch (JsonProcessingException e) {
        throw new ValidationException(CommonErrorCode.INVALID_PARAMETER, "entries",
            "Failed to serialize entries to JSON");
      }
    }
    throw new ValidationException(CommonErrorCode.INVALID_PARAMETER, "type",
        "Unsupported config resource type: " + resource.getType());
  }

  @Transactional
  public ConfigRelease rollback(Long targetReleaseId, String comment) {
    ConfigRelease targetRelease = configReleaseRepository.findById(targetReleaseId)
        .orElseThrow(() -> new NotFoundException(ConfigReleaseErrorCode.CONFIG_RELEASE_NOT_FOUND,
            "Release not found: " + targetReleaseId));

    Long resourceId = targetRelease.getConfigResourceId();
    Long environmentId = targetRelease.getEnvironmentId();

    ConfigRelease latestRelease = configReleaseRepository
        .findTopByConfigResourceIdAndEnvironmentIdOrderByVersionDesc(resourceId, environmentId)
        .orElseThrow(() -> new NotFoundException(ConfigReleaseErrorCode.CONFIG_RELEASE_NOT_FOUND,
            "No latest release found"));

    if (targetRelease.getSnapshot().equals(latestRelease.getSnapshot())) {
      return latestRelease;
    }

    ConfigRelease rollbackRelease = new ConfigRelease();
    rollbackRelease.setConfigResourceId(resourceId);
    rollbackRelease.setEnvironmentId(environmentId);
    rollbackRelease.setSnapshot(targetRelease.getSnapshot());
    rollbackRelease.setVersion(latestRelease.getVersion() + 1);
    rollbackRelease.setType("ROLLBACK");
    rollbackRelease.setComment(comment);
    rollbackRelease.setCreatedAt(Instant.now());
    rollbackRelease.setUpdatedAt(Instant.now());
    rollbackRelease.setDeleted(false);

    ConfigRelease savedRelease = configReleaseRepository.save(rollbackRelease);

    ConfigReleaseHistory history = new ConfigReleaseHistory();
    history.setConfigResourceId(resourceId);
    history.setEnvironmentId(environmentId);
    history.setReleaseId(savedRelease.getId());
    history.setPreviousReleaseId(latestRelease.getId());
    history.setCreatedAt(Instant.now());
    configReleaseHistoryRepository.save(history);

    saveReleaseMessage(savedRelease);
    notifyWatchers(resourceId, environmentId);

    auditLogService.log(null, "CONFIG_RELEASE", "ROLLBACK", savedRelease.getId().toString(),
        "Original Release: " + targetReleaseId + ", New Version: " + savedRelease.getVersion());

    return savedRelease;
  }

  private void notifyWatchers(Long resourceId, Long environmentId) {
    ConfigResource resource = configResourceService.get(resourceId);
    Application app = applicationService.get(resource.getApplicationId());
    Environment env = environmentService.get(environmentId);
    String appId = app.getAppId();
    String envCode = env.getCode();
    String configName = resource.getName();

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          configNotificationService.notifyWatchers(appId, envCode, configName);
        }
      });
    } else {
      configNotificationService.notifyWatchers(appId, envCode, configName);
    }
  }

  private void saveReleaseMessage(ConfigRelease release) {
    ConfigResource resource = configResourceService.get(release.getConfigResourceId());
    Application app = applicationService.get(resource.getApplicationId());
    Environment env = environmentService.get(release.getEnvironmentId());

    ReleaseMessage message = new ReleaseMessage();
    message.setReleaseId(release.getId());
    message.setConfigResourceId(release.getConfigResourceId());
    message.setEnvironmentId(release.getEnvironmentId());
    message.setAppId(app.getAppId());
    message.setEnvCode(env.getCode());
    message.setName(resource.getName());
    message.setCreatedAt(Instant.now());
    releaseMessageRepository.save(message);
  }

  public Optional<ConfigRelease> findLatest(Long resourceId, Long environmentId) {
    return configReleaseRepository.findTopByConfigResourceIdAndEnvironmentIdOrderByVersionDesc(
        resourceId, environmentId);
  }

  public Optional<ConfigRelease> findById(Long releaseId) {
    return configReleaseRepository.findById(releaseId);
  }
}
