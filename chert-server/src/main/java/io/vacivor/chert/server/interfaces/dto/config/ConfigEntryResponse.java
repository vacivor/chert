package io.vacivor.chert.server.interfaces.dto.config;

import io.vacivor.chert.server.domain.config.ConfigEntry;

public record ConfigEntryResponse(
    Long id,
    String key,
    String value,
    String valueType,
    String description
) {
  public static ConfigEntryResponse from(ConfigEntry entry) {
    return new ConfigEntryResponse(
        entry.getId(),
        entry.getKey(),
        entry.getValue(),
        entry.getValueType(),
        entry.getDescription()
    );
  }
}
