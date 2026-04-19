package io.vacivor.chert.server.interfaces.dto.config;

import io.vacivor.chert.server.domain.config.ConfigContent;
import java.time.Instant;

public record ConfigContentResponse(
    Long id,
    Long resourceId,
    Long environmentId,
    String content,
    Instant createdAt,
    Instant updatedAt) {

  public static ConfigContentResponse from(ConfigContent configContent) {
    return new ConfigContentResponse(
        configContent.getId(),
        configContent.getConfigResourceId(),
        configContent.getEnvironmentId(),
        configContent.getContent(),
        configContent.getCreatedAt(),
        configContent.getUpdatedAt());
  }
}
