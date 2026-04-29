package io.vacivor.chert.server.interfaces.dto.rbac;

public record PermissionCreateRequest(
    String code,
    String name,
    String description
) {
}
