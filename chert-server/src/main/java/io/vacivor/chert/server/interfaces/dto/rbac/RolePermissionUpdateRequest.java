package io.vacivor.chert.server.interfaces.dto.rbac;

import java.util.Set;

public record RolePermissionUpdateRequest(
    Set<String> permissions
) {
}
