package io.vacivor.chert.server.interfaces.dto.environment;

public record EnvironmentUpdateRequest(
    String code,
    String name,
    String description) {
}
