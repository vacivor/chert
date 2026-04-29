package io.vacivor.chert.server.application.user;

import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.application.rbac.RbacDefaults;
import io.vacivor.chert.server.application.rbac.RoleService;
import io.vacivor.chert.server.domain.rbac.Role;
import io.vacivor.chert.server.domain.user.User;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.UserErrorCode;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.infrastructure.persistence.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._-]{2,31}$");
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final int MIN_PASSWORD_LENGTH = 8;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditLogService auditLogService;
  private final RoleService roleService;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuditLogService auditLogService,
      RoleService roleService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.auditLogService = auditLogService;
    this.roleService = roleService;
  }

  @Transactional
  public User register(String username, String email, String rawPassword) {
    Set<String> roleCodes = new LinkedHashSet<>();
    roleCodes.add(RbacDefaults.ROLE_USER);
    if (userRepository.count() == 0) {
      roleCodes.add(RbacDefaults.ROLE_SUPER_ADMIN);
    }
    return createInternal(username, email, rawPassword, roleCodes, "REGISTER:" + username);
  }

  @Transactional
  public User create(String username, String email, String rawPassword, Set<String> roleCodes, String operator) {
    return createInternal(username, email, rawPassword, roleCodes, operator);
  }

  public User get(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(
            () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND, "User not found: " + id));
    initializeAccessControl(user);
    return user;
  }

  public User getByUsername(String username) {
    return userRepository.findByUsernameAndIsDeletedFalse(username)
        .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND,
            "User not found: " + username));
  }

  public Page<User> list(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);
    users.forEach(this::initializeAccessControl);
    return users;
  }

  @Transactional
  public User update(Long id, String rawPassword, Set<String> roleCodes, String operator) {
    User user = get(id);
    if (rawPassword != null) {
      validatePassword(rawPassword);
      user.setPassword(passwordEncoder.encode(rawPassword));
    }
    if (roleCodes != null && !roleCodes.isEmpty()) {
      user.setRoles(new LinkedHashSet<>(roleService.getByCodes(roleCodes)));
    }
    User saved = userRepository.save(user);
    initializeAccessControl(saved);
    auditLogService.log(operator, "USER", "UPDATE", saved.getId().toString(),
        "Updated user " + saved.getUsername());
    return saved;
  }

  @Transactional
  public void recordSuccessfulLogin(String username) {
    User user = getByUsername(username);
    auditLogService.log(username, "USER", "LOGIN", user.getId().toString(), "User logged in");
  }

  @Transactional
  public void recordLogout(String username) {
    User user = getByUsername(username);
    auditLogService.log(username, "USER", "LOGOUT", user.getId().toString(), "User logged out");
  }

  private User createInternal(
      String username,
      String email,
      String rawPassword,
      Set<String> roleCodes,
      String operator) {
    validateUsername(username);
    validateEmail(email);
    validatePassword(rawPassword);
    validateRoleCodes(roleCodes);

    if (userRepository.existsByUsernameAndIsDeletedFalse(username)) {
      throw new ConflictException(UserErrorCode.USER_USERNAME_DUPLICATED,
          "User with username '" + username + "' already exists");
    }
    if (userRepository.existsByEmailAndIsDeletedFalse(email.trim())) {
      throw new ConflictException(UserErrorCode.USER_EMAIL_DUPLICATED,
          "User with email '" + email + "' already exists");
    }

    User user = new User();
    user.setUsername(username.trim());
    user.setEmail(email.trim());
    user.setPassword(passwordEncoder.encode(rawPassword));
    user.setRoles(new LinkedHashSet<>(roleService.getByCodes(roleCodes)));
    user.setDeleted(false);

    User saved = userRepository.save(user);
    initializeAccessControl(saved);
    auditLogService.log(operator, "USER", "CREATE", saved.getId().toString(),
        "Created user " + saved.getUsername());
    return saved;
  }

  private void validateUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new ValidationException(UserErrorCode.USER_USERNAME_REQUIRED, "username",
          "Username cannot be blank");
    }
    if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
      throw new ValidationException(UserErrorCode.USER_INVALID_USERNAME, "username",
          "Username must start with a letter and contain only letters, numbers, dots, underscores, or hyphens");
    }
  }

  private void validateEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new ValidationException(UserErrorCode.USER_EMAIL_REQUIRED, "email",
          "Email cannot be blank");
    }
    if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
      throw new ValidationException(UserErrorCode.USER_INVALID_EMAIL, "email",
          "Email format is invalid");
    }
  }

  private void validatePassword(String rawPassword) {
    if (rawPassword == null || rawPassword.isBlank()) {
      throw new ValidationException(UserErrorCode.USER_PASSWORD_REQUIRED, "password",
          "Password cannot be blank");
    }
    if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
      throw new ValidationException(UserErrorCode.USER_INVALID_PASSWORD, "password",
          "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
    }
  }

  private void validateRoleCodes(Set<String> roleCodes) {
    if (roleCodes == null || roleCodes.isEmpty()) {
      throw new ValidationException(UserErrorCode.USER_ROLE_REQUIRED, "roles",
          "At least one role is required");
    }
  }

  private void initializeAccessControl(User user) {
    Hibernate.initialize(user.getRoles());
    user.getRoles().forEach(role -> Hibernate.initialize(role.getPermissions()));
  }
}
