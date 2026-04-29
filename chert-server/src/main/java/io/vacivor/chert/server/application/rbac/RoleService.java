package io.vacivor.chert.server.application.rbac;

import io.vacivor.chert.server.domain.rbac.Role;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.RoleErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.infrastructure.persistence.rbac.RoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleService {

  private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

  private final RoleRepository roleRepository;
  private final PermissionService permissionService;

  public RoleService(RoleRepository roleRepository, PermissionService permissionService) {
    this.roleRepository = roleRepository;
    this.permissionService = permissionService;
  }

  public List<Role> list() {
    return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
  }

  public List<Role> getByCodes(Set<String> codes) {
    if (codes == null || codes.isEmpty()) {
      return List.of();
    }
    Set<String> normalizedCodes = new LinkedHashSet<>();
    for (String code : codes) {
      normalizedCodes.add(validateCode(code));
    }
    List<Role> roles = roleRepository.findByCodeInAndIsDeletedFalse(normalizedCodes);
    if (roles.size() != normalizedCodes.size()) {
      throw new NotFoundException(RoleErrorCode.ROLE_NOT_FOUND,
          "Some roles were not found: " + normalizedCodes);
    }
    return roles;
  }

  @Transactional
  public Role create(String code, String name, String description, Set<String> permissionCodes) {
    String normalizedCode = validateCode(code);
    if (roleRepository.existsByCodeAndIsDeletedFalse(normalizedCode)) {
      throw new ConflictException(RoleErrorCode.ROLE_CODE_DUPLICATED,
          "Role with code '" + normalizedCode + "' already exists");
    }
    Role role = new Role();
    role.setCode(normalizedCode);
    role.setName((name == null || name.isBlank()) ? normalizedCode : name.trim());
    role.setDescription(description);
    role.setPermissions(new LinkedHashSet<>(permissionService.getByCodes(permissionCodes)));
    role.setDeleted(false);
    return roleRepository.save(role);
  }

  @Transactional
  public Role updatePermissions(Long id, Set<String> permissionCodes) {
    Role role = roleRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(RoleErrorCode.ROLE_NOT_FOUND,
            "Role not found: " + id));
    role.setPermissions(new LinkedHashSet<>(permissionService.getByCodes(permissionCodes)));
    return roleRepository.save(role);
  }

  private String validateCode(String code) {
    if (code == null || code.isBlank()) {
      throw new ValidationException(RoleErrorCode.ROLE_CODE_REQUIRED, "code",
          "Role code cannot be blank");
    }
    String normalizedCode = code.trim();
    if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
      throw new ValidationException(RoleErrorCode.ROLE_INVALID_CODE, "code",
          "Role code must start with an uppercase letter and contain only uppercase letters, numbers, or underscores");
    }
    return normalizedCode;
  }
}
