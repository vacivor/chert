package io.vacivor.chert.server.interfaces.dto.config;

public record ConfigReleaseRequest(
    String operator,
    String comment
) {
}
