package io.vacivor.chert.server.interfaces.dto.app;

import io.vacivor.chert.server.domain.app.ApplicationSecret;
import io.vacivor.chert.server.domain.app.ApplicationSecretStatusEnum;
import java.time.Instant;

public record ApplicationSecretResponse(
    Long id,
    Long appId,
    ApplicationSecretStatusEnum status,
    String name,
    String accessKey,
    String secretKey,
    String secretHint,
    Instant expiresAt,
    Instant lastUsedAt,
    Instant createdAt,
    Instant updatedAt
) {
  public static ApplicationSecretResponse from(ApplicationSecret secret) {
    return new ApplicationSecretResponse(
        secret.getId(),
        secret.getAppId(),
        secret.getStatus(),
        secret.getName(),
        secret.getAccessKey(),
        secret.getSecretKey(),
        secret.getSecretHint(),
        secret.getExpiresAt(),
        secret.getLastUsedAt(),
        secret.getCreatedAt(),
        secret.getUpdatedAt()
    );
  }
}