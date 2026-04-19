package io.vacivor.chert.server.interfaces.dto.config;

import io.vacivor.chert.server.domain.config.ConfigRelease;
import java.time.Instant;

public record ConfigReleaseResponse(
    Long id,
    Long configResourceId,
    Long environmentId,
    String type,
    String snapshot,
    Long version,
    String comment,
    Instant createdAt) {

  public static ConfigReleaseResponse from(ConfigRelease release) {
    return new ConfigReleaseResponse(
        release.getId(),
        release.getConfigResourceId(),
        release.getEnvironmentId(),
        release.getType(),
        release.getSnapshot(),
        release.getVersion(),
        release.getComment(),
        release.getCreatedAt());
  }
}
