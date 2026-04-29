package io.vacivor.chert.server.interfaces.dto.rbac;

import io.vacivor.chert.server.domain.rbac.Role;

public record RoleResponse(
    Long id,
    String code,
    String name,
    String description,
    java.util.List<String> permissions
) {

  public static RoleResponse from(Role role) {
    return new RoleResponse(
        role.getId(),
        role.getCode(),
        role.getName(),
        role.getDescription(),
        role.getPermissions().stream().map(permission -> permission.getCode()).sorted().toList());
  }
}
