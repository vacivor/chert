package io.vacivor.chert.server.application.rbac;

import java.util.List;

public final class RbacDefaults {

  public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
  public static final String ROLE_USER = "USER";

  public static final String PERMISSION_CONSOLE_ACCESS = "console:access";
  public static final String PERMISSION_USER_MANAGE = "user:manage";
  public static final String PERMISSION_RBAC_MANAGE = "rbac:manage";

  public static final List<String> SUPER_ADMIN_PERMISSIONS = List.of(
      PERMISSION_CONSOLE_ACCESS,
      PERMISSION_USER_MANAGE,
      PERMISSION_RBAC_MANAGE);

  public static final List<String> USER_PERMISSIONS = List.of(
      PERMISSION_CONSOLE_ACCESS);

  private RbacDefaults() {
  }
}
