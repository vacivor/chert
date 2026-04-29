package io.vacivor.chert.server.security;

import io.vacivor.chert.server.domain.rbac.Permission;
import io.vacivor.chert.server.domain.rbac.Role;
import io.vacivor.chert.server.domain.user.User;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

  private final Long id;
  private final String username;
  private final String passwordHash;
  private final Set<String> roleCodes;
  private final Set<String> permissionCodes;

  private AuthenticatedUser(
      Long id,
      String username,
      String passwordHash,
      Set<String> roleCodes,
      Set<String> permissionCodes) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
    this.roleCodes = roleCodes;
    this.permissionCodes = permissionCodes;
  }

  public static AuthenticatedUser from(User user) {
    Set<String> roleCodes = new LinkedHashSet<>();
    Set<String> permissionCodes = new LinkedHashSet<>();
    for (Role role : user.getRoles()) {
      roleCodes.add(role.getCode());
      for (Permission permission : role.getPermissions()) {
        permissionCodes.add(permission.getCode());
      }
    }
    return new AuthenticatedUser(
        user.getId(),
        user.getUsername(),
        user.getPassword(),
        roleCodes,
        permissionCodes);
  }

  public Long getId() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();
    for (String roleCode : roleCodes) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }
    for (String permissionCode : permissionCodes) {
      authorities.add(new SimpleGrantedAuthority(permissionCode));
    }
    return authorities;
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
