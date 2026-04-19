package io.vacivor.chert.server.interfaces.dto.app;

import io.vacivor.chert.server.domain.app.ApplicationSecretStatusEnum;

public record ApplicationSecretStatusUpdateRequest(
    ApplicationSecretStatusEnum status
) {
}