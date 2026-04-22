package io.vacivor.chert.server.interfaces.openapi;

import java.util.List;

public record ConfigNotificationResponse(
    Long lastMessageId,
    List<String> configNames
) {
}
