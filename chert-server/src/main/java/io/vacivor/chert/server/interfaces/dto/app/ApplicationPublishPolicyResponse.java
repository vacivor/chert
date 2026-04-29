package io.vacivor.chert.server.interfaces.dto.app;

import io.vacivor.chert.server.domain.app.ApplicationPublishPolicy;
import java.time.Instant;

public record ApplicationPublishPolicyResponse(
    Long id,
    Long applicationId,
    Long environmentId,
    boolean publishRequiresApproval,
    Instant createdAt,
    Instant updatedAt
) {

  public static ApplicationPublishPolicyResponse from(ApplicationPublishPolicy policy) {
    return new ApplicationPublishPolicyResponse(
        policy.getId(),
        policy.getApplicationId(),
        policy.getEnvironmentId(),
        Boolean.TRUE.equals(policy.getPublishRequiresApproval()),
        policy.getCreatedAt(),
        policy.getUpdatedAt());
  }
}
