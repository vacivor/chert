package io.vacivor.chert.server.interfaces.admin.app;

import io.vacivor.chert.server.application.app.ApplicationPublishPolicyService;
import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationPublishPolicyRequest;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationPublishPolicyResponse;
import io.vacivor.chert.server.security.ApplicationAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/applications/{appId}/environments/{environmentId}/publish-policy")
public class ApplicationPublishPolicyAdminController {

  private final ApplicationPublishPolicyService applicationPublishPolicyService;
  private final ApplicationAccessService applicationAccessService;

  public ApplicationPublishPolicyAdminController(
      ApplicationPublishPolicyService applicationPublishPolicyService,
      ApplicationAccessService applicationAccessService) {
    this.applicationPublishPolicyService = applicationPublishPolicyService;
    this.applicationAccessService = applicationAccessService;
  }

  @GetMapping
  public ResponseEntity<ApplicationPublishPolicyResponse> get(
      @PathVariable Long appId,
      @PathVariable Long environmentId) {
    applicationAccessService.requireApplicationMember(appId);
    return ResponseEntity.ok(ApplicationPublishPolicyResponse.from(
        applicationPublishPolicyService.getEffective(appId, environmentId)));
  }

  @PutMapping
  public ResponseEntity<ApplicationPublishPolicyResponse> save(
      @PathVariable Long appId,
      @PathVariable Long environmentId,
      @RequestBody ApplicationPublishPolicyRequest request) {
    applicationAccessService.requireApplicationManager(appId);
    if (request.publishRequiresApproval() == null) {
      throw new ValidationException(
          CommonErrorCode.INVALID_PARAMETER,
          "publishRequiresApproval",
          "publishRequiresApproval cannot be null");
    }
    return ResponseEntity.ok(ApplicationPublishPolicyResponse.from(
        applicationPublishPolicyService.save(appId, environmentId, request.publishRequiresApproval())));
  }
}
