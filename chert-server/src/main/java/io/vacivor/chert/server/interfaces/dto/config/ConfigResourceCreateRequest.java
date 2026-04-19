package io.vacivor.chert.server.interfaces.dto.config;

import io.vacivor.chert.server.common.ConfigFormat;
import io.vacivor.chert.server.common.ConfigType;

public record ConfigResourceCreateRequest(
    String configName,
    ConfigType type,
    ConfigFormat format,
    Long version,
    String description) {
}
