package io.vacivor.chert.server.interfaces.admin.config;

import io.vacivor.chert.server.application.app.ApplicationPublishPolicyService;
import io.vacivor.chert.server.application.config.ConfigDiffService;
import io.vacivor.chert.server.application.config.ConfigReleaseHistoryService;
import io.vacivor.chert.server.application.config.ConfigReleaseRequestService;
import io.vacivor.chert.server.application.config.ConfigReleaseService;
import io.vacivor.chert.server.application.config.ConfigResourceService;
import io.vacivor.chert.server.interfaces.dto.config.ConfigDiffResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleasePublishResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseRequestResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseReviewRequest;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseHistoryResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseRequest;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseSubmissionRequest;
import io.vacivor.chert.server.security.ApplicationAccessService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/config-resources/{resourceId}/environments/{environmentId}/releases")
public class ConfigReleaseAdminController {

  private final ApplicationPublishPolicyService applicationPublishPolicyService;
  private final ConfigReleaseService configReleaseService;
  private final ConfigReleaseRequestService configReleaseRequestService;
  private final ConfigReleaseHistoryService configReleaseHistoryService;
  private final ConfigDiffService configDiffService;
  private final ConfigResourceService configResourceService;
  private final ApplicationAccessService applicationAccessService;

  public ConfigReleaseAdminController(
      ApplicationPublishPolicyService applicationPublishPolicyService,
      ConfigReleaseService configReleaseService,
      ConfigReleaseRequestService configReleaseRequestService,
      ConfigReleaseHistoryService configReleaseHistoryService,
      ConfigDiffService configDiffService,
      ConfigResourceService configResourceService,
      ApplicationAccessService applicationAccessService) {
    this.applicationPublishPolicyService = applicationPublishPolicyService;
    this.configReleaseService = configReleaseService;
    this.configReleaseRequestService = configReleaseRequestService;
    this.configReleaseHistoryService = configReleaseHistoryService;
    this.configDiffService = configDiffService;
    this.configResourceService = configResourceService;
    this.applicationAccessService = applicationAccessService;
  }

  @GetMapping("/latest")
  public ResponseEntity<ConfigReleaseResponse> getLatest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    applicationAccessService.requireResourceMember(resourceId);
    return configReleaseService.findLatest(resourceId, environmentId)
        .map(ConfigReleaseResponse::from)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<ConfigReleasePublishResponse> publish(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @RequestBody ConfigReleaseRequest request) {
    if (applicationAccessService.canManageResource(resourceId)) {
      return ResponseEntity.ok(ConfigReleasePublishResponse.published(
          ConfigReleaseResponse.from(
              configReleaseService.publish(
                  resourceId,
                  environmentId,
                  applicationAccessService.currentUsername(),
                  request.comment()))));
    }

    applicationAccessService.requireResourceMember(resourceId);
    Long applicationId = configResourceService.get(resourceId).getApplicationId();
    boolean publishRequiresApproval = applicationPublishPolicyService
        .publishRequiresApproval(applicationId, environmentId);
    if (publishRequiresApproval) {
      return ResponseEntity.accepted().body(ConfigReleasePublishResponse.submitted(
          ConfigReleaseRequestResponse.from(
              configReleaseRequestService.submit(
                  resourceId,
                  environmentId,
                  applicationAccessService.currentUsername(),
                  request.comment()))));
    }

    return ResponseEntity.ok(ConfigReleasePublishResponse.published(
        ConfigReleaseResponse.from(
            configReleaseService.publish(
                resourceId,
                environmentId,
                applicationAccessService.currentUsername(),
                request.comment()))));
  }

  @GetMapping("/requests")
  public ResponseEntity<List<ConfigReleaseRequestResponse>> listRequests(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    applicationAccessService.requireResourceMember(resourceId);
    return ResponseEntity.ok(configReleaseRequestService.list(resourceId, environmentId).stream()
        .map(ConfigReleaseRequestResponse::from)
        .toList());
  }

  @PostMapping("/requests")
  public ResponseEntity<ConfigReleaseRequestResponse> submitReleaseRequest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @RequestBody ConfigReleaseSubmissionRequest request) {
    applicationAccessService.requireResourceMember(resourceId);
    return ResponseEntity.ok(ConfigReleaseRequestResponse.from(
        configReleaseRequestService.submit(
            resourceId,
            environmentId,
            applicationAccessService.currentUsername(),
            request.comment())));
  }

  @PostMapping("/requests/{requestId}/approve")
  public ResponseEntity<ConfigReleaseRequestResponse> approveReleaseRequest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @PathVariable Long requestId,
      @RequestBody ConfigReleaseReviewRequest request) {
    applicationAccessService.requireResourceManager(resourceId);
    return ResponseEntity.ok(ConfigReleaseRequestResponse.from(
        configReleaseRequestService.approve(
            requestId,
            applicationAccessService.currentUsername(),
            request.comment())));
  }

  @PostMapping("/requests/{requestId}/reject")
  public ResponseEntity<ConfigReleaseRequestResponse> rejectReleaseRequest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @PathVariable Long requestId,
      @RequestBody ConfigReleaseReviewRequest request) {
    applicationAccessService.requireResourceManager(resourceId);
    return ResponseEntity.ok(ConfigReleaseRequestResponse.from(
        configReleaseRequestService.reject(
            requestId,
            applicationAccessService.currentUsername(),
            request.comment())));
  }

  @PostMapping("/requests/{requestId}/withdraw")
  public ResponseEntity<ConfigReleaseRequestResponse> withdrawReleaseRequest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @PathVariable Long requestId,
      @RequestBody ConfigReleaseSubmissionRequest request) {
    applicationAccessService.requireResourceMember(resourceId);
    return ResponseEntity.ok(ConfigReleaseRequestResponse.from(
        configReleaseRequestService.withdraw(
            requestId,
            applicationAccessService.currentUsername(),
            applicationAccessService.canManageResource(resourceId),
            request.comment())));
  }

  @PostMapping("/requests/{requestId}/resubmit")
  public ResponseEntity<ConfigReleaseRequestResponse> resubmitReleaseRequest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @PathVariable Long requestId,
      @RequestBody ConfigReleaseSubmissionRequest request) {
    applicationAccessService.requireResourceMember(resourceId);
    return ResponseEntity.ok(ConfigReleaseRequestResponse.from(
        configReleaseRequestService.resubmit(
            requestId,
            applicationAccessService.currentUsername(),
            applicationAccessService.canManageResource(resourceId),
            request.comment())));
  }

  @PostMapping("/{releaseId}/rollback")
  public ResponseEntity<ConfigReleaseResponse> rollback(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @PathVariable Long releaseId,
      @RequestBody ConfigReleaseRequest request) {
    applicationAccessService.requireResourceManager(resourceId);
    return ResponseEntity.ok(ConfigReleaseResponse.from(
        configReleaseService.rollback(
            releaseId,
            applicationAccessService.currentUsername(),
            request.comment())));
  }

  @GetMapping("/history")
  public ResponseEntity<List<ConfigReleaseHistoryResponse>> listHistory(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    applicationAccessService.requireResourceMember(resourceId);
    return ResponseEntity.ok(configReleaseHistoryService.list(resourceId, environmentId).stream()
        .map(ConfigReleaseHistoryResponse::from)
        .toList());
  }

  @GetMapping("/diff")
  public ResponseEntity<ConfigDiffResponse> diff(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @RequestParam Long baseReleaseId,
      @RequestParam Long targetReleaseId) {
    applicationAccessService.requireResourceMember(resourceId);
    ConfigDiffService.DiffResult result = configDiffService.diffReleases(baseReleaseId, targetReleaseId);
    return ResponseEntity.ok(new ConfigDiffResponse(
        result.oldContent(),
        result.newContent(),
        result.hasChanges()));
  }

}
