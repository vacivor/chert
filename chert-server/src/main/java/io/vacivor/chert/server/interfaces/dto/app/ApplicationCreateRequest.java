package io.vacivor.chert.server.interfaces.dto.app;

public record ApplicationCreateRequest(
    String appId,
    String name,
    String description,
    Long ownerUserId,
    Long maintainerUserId,
    java.util.Set<Long> developerUserIds) {
}
