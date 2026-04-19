package io.vacivor.chert.server.interfaces.dto.environment;

import io.vacivor.chert.server.domain.environment.Environment;
import java.time.Instant;

public record EnvironmentResponse(
    Long id,
    String code,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt) {

  public static EnvironmentResponse from(Environment environment) {
    return new EnvironmentResponse(
        environment.getId(),
        environment.getCode(),
        environment.getName(),
        environment.getDescription(),
        environment.getCreatedAt(),
        environment.getUpdatedAt());
  }
}
