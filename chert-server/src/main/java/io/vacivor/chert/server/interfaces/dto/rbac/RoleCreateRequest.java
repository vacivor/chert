package io.vacivor.chert.server.interfaces.dto.rbac;

import java.util.Set;

public record RoleCreateRequest(
    String code,
    String name,
    String description,
    Set<String> permissions
) {
}
