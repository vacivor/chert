package io.vacivor.chert.server.interfaces.dto.config;

import io.vacivor.chert.server.common.ConfigFormat;
import io.vacivor.chert.server.common.ConfigType;
import io.vacivor.chert.server.domain.config.ConfigResource;
import java.time.Instant;

public record ConfigResourceResponse(
    Long id,
    Long applicationId,
    String name,
    ConfigType type,
    ConfigFormat format,
    Long version,
    String description,
    Instant createdAt,
    Instant updatedAt) {

  public static ConfigResourceResponse from(ConfigResource configResource) {
    return new ConfigResourceResponse(
        configResource.getId(),
        configResource.getApplicationId(),
        configResource.getName(),
        configResource.getType(),
        configResource.getFormat(),
        configResource.getVersion(),
        configResource.getDescription(),
        configResource.getCreatedAt(),
        configResource.getUpdatedAt());
  }
}
