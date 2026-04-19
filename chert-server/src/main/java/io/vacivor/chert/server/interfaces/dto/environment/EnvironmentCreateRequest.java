package io.vacivor.chert.server.interfaces.dto.environment;

public record EnvironmentCreateRequest(
    String code,
    String name,
    String description) {
}
