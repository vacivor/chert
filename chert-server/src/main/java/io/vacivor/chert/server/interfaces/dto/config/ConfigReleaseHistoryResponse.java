package io.vacivor.chert.server.interfaces.dto.config;

import io.vacivor.chert.server.domain.config.ConfigReleaseHistory;
import java.time.Instant;

public record ConfigReleaseHistoryResponse(
    Long id,
    Long configResourceId,
    Long environmentId,
    Long releaseId,
    Long previousReleaseId,
    Instant createdAt) {

  public static ConfigReleaseHistoryResponse from(ConfigReleaseHistory history) {
    return new ConfigReleaseHistoryResponse(
        history.getId(),
        history.getConfigResourceId(),
        history.getEnvironmentId(),
        history.getReleaseId(),
        history.getPreviousReleaseId(),
        history.getCreatedAt());
  }
}
