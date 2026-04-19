package io.vacivor.chert.client;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ChertConfigResponse(String content, Instant updatedAt, ConfigType type, ConfigFormat format) {

  public ChertConfigResponse {
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(format, "format must not be null");
  }

}
