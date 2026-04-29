package io.vacivor.chert.server.interfaces.dto.config;

import io.vacivor.chert.server.domain.config.ConfigReleaseRequest;
import io.vacivor.chert.server.domain.config.ConfigReleaseRequestStatus;
import java.time.Instant;

public record ConfigReleaseRequestResponse(
    Long id,
    Long configResourceId,
    Long environmentId,
    ConfigReleaseRequestStatus status,
    String snapshot,
    String requestComment,
    String requestedBy,
    String reviewComment,
    String reviewedBy,
    Instant reviewedAt,
    Long approvedReleaseId,
    Instant createdAt
) {

  public static ConfigReleaseRequestResponse from(ConfigReleaseRequest request) {
    return new ConfigReleaseRequestResponse(
        request.getId(),
        request.getConfigResourceId(),
        request.getEnvironmentId(),
        request.getStatus(),
        request.getSnapshot(),
        request.getRequestComment(),
        request.getRequestedBy(),
        request.getReviewComment(),
        request.getReviewedBy(),
        request.getReviewedAt(),
        request.getApprovedReleaseId(),
        request.getCreatedAt());
  }
}
