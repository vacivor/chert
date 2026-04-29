package io.vacivor.chert.server.application.rbac;

import io.vacivor.chert.server.domain.rbac.Permission;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.PermissionErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.infrastructure.persistence.rbac.PermissionRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissionService {

  private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9:_-]{2,63}$");

  private final PermissionRepository permissionRepository;

  public PermissionService(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  public List<Permission> list() {
    return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
  }

  public List<Permission> getByCodes(Set<String> codes) {
    if (codes == null || codes.isEmpty()) {
      return List.of();
    }
    Set<String> normalizedCodes = new LinkedHashSet<>();
    for (String code : codes) {
      normalizedCodes.add(validateCode(code));
    }
    List<Permission> permissions = permissionRepository.findByCodeInAndIsDeletedFalse(normalizedCodes);
    if (permissions.size() != normalizedCodes.size()) {
      throw new NotFoundException(PermissionErrorCode.PERMISSION_NOT_FOUND,
          "Some permissions were not found: " + normalizedCodes);
    }
    return permissions;
  }

  @Transactional
  public Permission create(String code, String name, String description) {
    String normalizedCode = validateCode(code);
    if (permissionRepository.existsByCodeAndIsDeletedFalse(normalizedCode)) {
      throw new ConflictException(PermissionErrorCode.PERMISSION_CODE_DUPLICATED,
          "Permission with code '" + normalizedCode + "' already exists");
    }
    Permission permission = new Permission();
    permission.setCode(normalizedCode);
    permission.setName((name == null || name.isBlank()) ? normalizedCode : name.trim());
    permission.setDescription(description);
    permission.setDeleted(false);
    return permissionRepository.save(permission);
  }

  private String validateCode(String code) {
    if (code == null || code.isBlank()) {
      throw new ValidationException(PermissionErrorCode.PERMISSION_CODE_REQUIRED, "code",
          "Permission code cannot be blank");
    }
    String normalizedCode = code.trim();
    if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
      throw new ValidationException(PermissionErrorCode.PERMISSION_INVALID_CODE, "code",
          "Permission code must start with a lowercase letter and contain only lowercase letters, numbers, colon, underscore, or hyphen");
    }
    return normalizedCode;
  }
}
