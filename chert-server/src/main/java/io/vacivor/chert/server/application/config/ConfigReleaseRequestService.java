package io.vacivor.chert.server.application.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.application.environment.EnvironmentService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.config.ConfigRelease;
import io.vacivor.chert.server.domain.config.ConfigReleaseRequest;
import io.vacivor.chert.server.domain.config.ConfigReleaseRequestStatus;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.ConfigReleaseRequestErrorCode;
import io.vacivor.chert.server.error.ForbiddenException;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigReleaseRequestRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfigReleaseRequestService {

  private final ConfigReleaseRequestRepository configReleaseRequestRepository;
  private final ConfigReleaseService configReleaseService;
  private final ConfigResourceService configResourceService;
  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;
  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;

  public ConfigReleaseRequestService(
      ConfigReleaseRequestRepository configReleaseRequestRepository,
      ConfigReleaseService configReleaseService,
      ConfigResourceService configResourceService,
      ApplicationService applicationService,
      EnvironmentService environmentService,
      AuditLogService auditLogService,
      ObjectMapper objectMapper) {
    this.configReleaseRequestRepository = configReleaseRequestRepository;
    this.configReleaseService = configReleaseService;
    this.configResourceService = configResourceService;
    this.applicationService = applicationService;
    this.environmentService = environmentService;
    this.auditLogService = auditLogService;
    this.objectMapper = objectMapper;
  }

  public List<ConfigReleaseRequest> list(Long resourceId, Long environmentId) {
    return configReleaseRequestRepository
        .findByConfigResourceIdAndEnvironmentIdOrderByCreatedAtDesc(resourceId, environmentId);
  }

  public ConfigReleaseRequest get(Long requestId) {
    return configReleaseRequestRepository.findById(requestId)
        .orElseThrow(() -> new NotFoundException(
            ConfigReleaseRequestErrorCode.CONFIG_RELEASE_REQUEST_NOT_FOUND,
            "Config release request not found: " + requestId));
  }

  @Transactional
  public ConfigReleaseRequest submit(Long resourceId, Long environmentId, String requestedBy, String comment) {
    ConfigResource resource = configResourceService.get(resourceId);
    Application application = applicationService.get(resource.getApplicationId());
    Environment environment = environmentService.get(environmentId);
    String snapshot = configReleaseService.captureCurrentSnapshot(resourceId, environmentId);

    return createRequest(resource, application, environment, snapshot, requestedBy, comment, "SUBMIT", null);
  }

  @Transactional
  public ConfigReleaseRequest withdraw(
      Long requestId,
      String operator,
      boolean canManageResource,
      String comment) {
    ConfigReleaseRequest releaseRequest = get(requestId);
    ensurePending(releaseRequest);
    ensureRequesterOrManager(releaseRequest, operator, canManageResource);

    ConfigResource resource = configResourceService.get(releaseRequest.getConfigResourceId());
    Application application = applicationService.get(resource.getApplicationId());
    Environment environment = environmentService.get(releaseRequest.getEnvironmentId());

    releaseRequest.setStatus(ConfigReleaseRequestStatus.WITHDRAWN);
    releaseRequest.setReviewedBy(operator);
    releaseRequest.setReviewComment(comment);
    releaseRequest.setReviewedAt(Instant.now());
    ConfigReleaseRequest savedRequest = configReleaseRequestRepository.save(releaseRequest);

    Map<String, Object> details = new LinkedHashMap<>();
    details.put("appId", application.getAppId());
    details.put("configName", resource.getName());
    details.put("environment", environment.getCode());
    details.put("requestId", savedRequest.getId());
    details.put("requestedBy", savedRequest.getRequestedBy());
    details.put("withdrawnBy", operator);
    details.put("requestComment", savedRequest.getRequestComment());
    details.put("withdrawComment", comment);
    details.put("snapshot", savedRequest.getSnapshot());
    auditLogService.log(
        operator,
        "CONFIG_RELEASE_REQUEST",
        "WITHDRAW",
        savedRequest.getId().toString(),
        toJsonDetails(details));
    return savedRequest;
  }

  @Transactional
  public ConfigReleaseRequest resubmit(
      Long requestId,
      String operator,
      boolean canManageResource,
      String comment) {
    ConfigReleaseRequest previousRequest = get(requestId);
    ensureResubmittable(previousRequest);
    ensureRequesterOrManager(previousRequest, operator, canManageResource);

    ConfigResource resource = configResourceService.get(previousRequest.getConfigResourceId());
    Application application = applicationService.get(resource.getApplicationId());
    Environment environment = environmentService.get(previousRequest.getEnvironmentId());
    String snapshot = configReleaseService.captureCurrentSnapshot(
        previousRequest.getConfigResourceId(),
        previousRequest.getEnvironmentId());

    return createRequest(
        resource,
        application,
        environment,
        snapshot,
        operator,
        comment,
        "RESUBMIT",
        previousRequest.getId());
  }

  private ConfigReleaseRequest createRequest(
      ConfigResource resource,
      Application application,
      Environment environment,
      String snapshot,
      String requestedBy,
      String comment,
      String auditAction,
      Long sourceRequestId) {
    Long resourceId = resource.getId();
    Long environmentId = environment.getId();

    configReleaseRequestRepository
        .findTopByConfigResourceIdAndEnvironmentIdAndStatusOrderByCreatedAtDesc(
            resourceId, environmentId, ConfigReleaseRequestStatus.PENDING)
        .ifPresent(existing -> {
          if (existing.getSnapshot().equals(snapshot)) {
            throw new ConflictException(
                ConfigReleaseRequestErrorCode.CONFIG_RELEASE_REQUEST_STATUS_INVALID,
                "An identical pending release request already exists");
          }
        });

    ConfigReleaseRequest releaseRequest = new ConfigReleaseRequest();
    releaseRequest.setConfigResourceId(resourceId);
    releaseRequest.setEnvironmentId(environmentId);
    releaseRequest.setSnapshot(snapshot);
    releaseRequest.setStatus(ConfigReleaseRequestStatus.PENDING);
    releaseRequest.setRequestComment(comment);
    releaseRequest.setRequestedBy(requestedBy);
    releaseRequest.setDeleted(false);

    ConfigReleaseRequest savedRequest = configReleaseRequestRepository.save(releaseRequest);

    Map<String, Object> details = new LinkedHashMap<>();
    details.put("appId", application.getAppId());
    details.put("configName", resource.getName());
    details.put("environment", environment.getCode());
    details.put("requestId", savedRequest.getId());
    details.put("requestedBy", requestedBy);
    details.put("reason", comment);
    details.put("snapshot", snapshot);
    if (sourceRequestId != null) {
      details.put("sourceRequestId", sourceRequestId);
    }
    auditLogService.log(
        requestedBy,
        "CONFIG_RELEASE_REQUEST",
        auditAction,
        savedRequest.getId().toString(),
        toJsonDetails(details));
    return savedRequest;
  }

  @Transactional
  public ConfigReleaseRequest approve(Long requestId, String reviewedBy, String comment) {
    ConfigReleaseRequest releaseRequest = get(requestId);
    ensurePending(releaseRequest);

    ConfigResource resource = configResourceService.get(releaseRequest.getConfigResourceId());
    Application application = applicationService.get(resource.getApplicationId());
    Environment environment = environmentService.get(releaseRequest.getEnvironmentId());

    ConfigRelease release = configReleaseService.publishSnapshot(
        releaseRequest.getConfigResourceId(),
        releaseRequest.getEnvironmentId(),
        releaseRequest.getSnapshot(),
        reviewedBy,
        releaseRequest.getRequestComment(),
        "APPROVED",
        "PUBLISH");

    releaseRequest.setStatus(ConfigReleaseRequestStatus.APPROVED);
    releaseRequest.setReviewedBy(reviewedBy);
    releaseRequest.setReviewComment(comment);
    releaseRequest.setReviewedAt(Instant.now());
    releaseRequest.setApprovedReleaseId(release.getId());
    ConfigReleaseRequest savedRequest = configReleaseRequestRepository.save(releaseRequest);

    Map<String, Object> details = new LinkedHashMap<>();
    details.put("appId", application.getAppId());
    details.put("configName", resource.getName());
    details.put("environment", environment.getCode());
    details.put("requestId", savedRequest.getId());
    details.put("releaseId", release.getId());
    details.put("version", release.getVersion());
    details.put("requestedBy", savedRequest.getRequestedBy());
    details.put("reviewedBy", reviewedBy);
    details.put("requestComment", savedRequest.getRequestComment());
    details.put("reviewComment", comment);
    details.put("snapshot", savedRequest.getSnapshot());
    auditLogService.log(
        reviewedBy,
        "CONFIG_RELEASE_REQUEST",
        "APPROVE",
        savedRequest.getId().toString(),
        toJsonDetails(details));
    return savedRequest;
  }

  @Transactional
  public ConfigReleaseRequest reject(Long requestId, String reviewedBy, String comment) {
    ConfigReleaseRequest releaseRequest = get(requestId);
    ensurePending(releaseRequest);

    ConfigResource resource = configResourceService.get(releaseRequest.getConfigResourceId());
    Application application = applicationService.get(resource.getApplicationId());
    Environment environment = environmentService.get(releaseRequest.getEnvironmentId());

    releaseRequest.setStatus(ConfigReleaseRequestStatus.REJECTED);
    releaseRequest.setReviewedBy(reviewedBy);
    releaseRequest.setReviewComment(comment);
    releaseRequest.setReviewedAt(Instant.now());
    ConfigReleaseRequest savedRequest = configReleaseRequestRepository.save(releaseRequest);

    Map<String, Object> details = new LinkedHashMap<>();
    details.put("appId", application.getAppId());
    details.put("configName", resource.getName());
    details.put("environment", environment.getCode());
    details.put("requestId", savedRequest.getId());
    details.put("requestedBy", savedRequest.getRequestedBy());
    details.put("reviewedBy", reviewedBy);
    details.put("requestComment", savedRequest.getRequestComment());
    details.put("reviewComment", comment);
    details.put("snapshot", savedRequest.getSnapshot());
    auditLogService.log(
        reviewedBy,
        "CONFIG_RELEASE_REQUEST",
        "REJECT",
        savedRequest.getId().toString(),
        toJsonDetails(details));
    return savedRequest;
  }

  private void ensurePending(ConfigReleaseRequest releaseRequest) {
    if (releaseRequest.getStatus() != ConfigReleaseRequestStatus.PENDING) {
      throw new ConflictException(
          ConfigReleaseRequestErrorCode.CONFIG_RELEASE_REQUEST_STATUS_INVALID,
          "Only pending release requests can be reviewed");
    }
  }

  private void ensureResubmittable(ConfigReleaseRequest releaseRequest) {
    if (releaseRequest.getStatus() != ConfigReleaseRequestStatus.REJECTED
        && releaseRequest.getStatus() != ConfigReleaseRequestStatus.WITHDRAWN) {
      throw new ConflictException(
          ConfigReleaseRequestErrorCode.CONFIG_RELEASE_REQUEST_STATUS_INVALID,
          "Only rejected or withdrawn release requests can be resubmitted");
    }
  }

  private void ensureRequesterOrManager(
      ConfigReleaseRequest releaseRequest,
      String operator,
      boolean canManageResource) {
    if (canManageResource) {
      return;
    }
    if (!releaseRequest.getRequestedBy().equals(operator)) {
      throw new ForbiddenException(
          CommonErrorCode.FORBIDDEN,
          "Only the requester or application owner/maintainer can perform this action");
    }
  }

  private String toJsonDetails(Map<String, Object> details) {
    try {
      return objectMapper.writeValueAsString(details);
    } catch (JsonProcessingException exception) {
      throw new ValidationException(
          CommonErrorCode.INVALID_PARAMETER,
          "details",
          "Failed to serialize audit details");
    }
  }
}
