package io.vacivor.chert.server.interfaces.dto.config;

public record ConfigEntryRequest(
    String key,
    String value,
    String valueType,
    String description
) {
}
