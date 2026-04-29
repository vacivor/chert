package io.vacivor.chert.server.interfaces.dto.rbac;

import io.vacivor.chert.server.domain.rbac.Permission;

public record PermissionResponse(
    Long id,
    String code,
    String name,
    String description
) {

  public static PermissionResponse from(Permission permission) {
    return new PermissionResponse(
        permission.getId(),
        permission.getCode(),
        permission.getName(),
        permission.getDescription());
  }
}
