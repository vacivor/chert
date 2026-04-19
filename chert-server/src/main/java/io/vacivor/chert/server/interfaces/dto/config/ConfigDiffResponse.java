package io.vacivor.chert.server.interfaces.dto.config;

public record ConfigDiffResponse(
    String oldContent,
    String newContent
) {
}
