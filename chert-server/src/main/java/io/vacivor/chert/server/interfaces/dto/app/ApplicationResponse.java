package io.vacivor.chert.server.interfaces.dto.app;

import io.vacivor.chert.server.domain.app.Application;
import java.time.Instant;

public record ApplicationResponse(
    Long id,
    String appId,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt) {

  public static ApplicationResponse from(Application application) {
    return new ApplicationResponse(
        application.getId(),
        application.getAppId(),
        application.getName(),
        application.getDescription(),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }
}
