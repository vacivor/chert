package io.vacivor.chert.server.application.rbac;

import io.vacivor.chert.server.infrastructure.persistence.rbac.PermissionRepository;
import io.vacivor.chert.server.infrastructure.persistence.rbac.RoleRepository;
import java.util.LinkedHashSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RbacBootstrap implements ApplicationRunner {

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final PermissionService permissionService;
  private final RoleService roleService;

  public RbacBootstrap(
      PermissionRepository permissionRepository,
      RoleRepository roleRepository,
      PermissionService permissionService,
      RoleService roleService) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
    this.permissionService = permissionService;
    this.roleService = roleService;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    ensurePermission(RbacDefaults.PERMISSION_CONSOLE_ACCESS, "Console Access");
    ensurePermission(RbacDefaults.PERMISSION_USER_MANAGE, "User Manage");
    ensurePermission(RbacDefaults.PERMISSION_RBAC_MANAGE, "RBAC Manage");

    ensureRole(RbacDefaults.ROLE_SUPER_ADMIN, "Super Administrator", new LinkedHashSet<>(RbacDefaults.SUPER_ADMIN_PERMISSIONS));
    ensureRole(RbacDefaults.ROLE_USER, "User", new LinkedHashSet<>(RbacDefaults.USER_PERMISSIONS));
  }

  private void ensurePermission(String code, String name) {
    if (!permissionRepository.existsByCodeAndIsDeletedFalse(code)) {
      permissionService.create(code, name, null);
    }
  }

  private void ensureRole(String code, String name, LinkedHashSet<String> permissionCodes) {
    if (!roleRepository.existsByCodeAndIsDeletedFalse(code)) {
      roleService.create(code, name, null, permissionCodes);
    }
  }
}
