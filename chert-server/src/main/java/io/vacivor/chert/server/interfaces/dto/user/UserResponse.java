package io.vacivor.chert.server.interfaces.dto.user;

import io.vacivor.chert.server.domain.rbac.Role;
import io.vacivor.chert.server.domain.user.User;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

public record UserResponse(
    Long id,
    String username,
    String email,
    List<String> roles,
    List<String> permissions,
    Instant createdAt
) {

  public static UserResponse from(User user) {
    List<String> roleCodes = user.getRoles().stream().map(Role::getCode).sorted().toList();
    LinkedHashSet<String> permissionCodes = new LinkedHashSet<>();
    user.getRoles().forEach(role -> role.getPermissions().forEach(permission -> permissionCodes.add(permission.getCode())));
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roleCodes,
        List.copyOf(permissionCodes),
        user.getCreatedAt());
  }
}
