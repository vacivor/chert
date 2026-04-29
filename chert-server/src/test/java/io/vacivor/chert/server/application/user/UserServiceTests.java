package io.vacivor.chert.server.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.application.rbac.RoleService;
import io.vacivor.chert.server.domain.rbac.Role;
import io.vacivor.chert.server.domain.user.User;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.infrastructure.persistence.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private AuditLogService auditLogService;
  @Mock
  private RoleService roleService;

  @InjectMocks
  private UserService userService;

  @Test
  void shouldRegisterUser() {
    when(userRepository.existsByUsernameAndIsDeletedFalse("admin")).thenReturn(false);
    when(userRepository.existsByEmailAndIsDeletedFalse("admin@example.com")).thenReturn(false);
    when(userRepository.count()).thenReturn(0L);
    when(passwordEncoder.encode("password123")).thenReturn("encoded");
    Role userRole = new Role();
    userRole.setCode("USER");
    Role adminRole = new Role();
    adminRole.setCode("SUPER_ADMIN");
    when(roleService.getByCodes(Set.of("USER", "SUPER_ADMIN"))).thenReturn(List.of(userRole, adminRole));
    when(userRepository.save(any())).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(1L);
      user.setCreatedAt(Instant.now());
      return user;
    });

    User user = userService.register("admin", "admin@example.com", "password123");

    assertThat(user.getUsername()).isEqualTo("admin");
    assertThat(user.getEmail()).isEqualTo("admin@example.com");
    assertThat(user.getPassword()).isEqualTo("encoded");
    assertThat(user.getRoles()).extracting(Role::getCode).containsExactlyInAnyOrder("USER", "SUPER_ADMIN");
  }

  @Test
  void shouldRejectDuplicatedUsername() {
    when(userRepository.existsByUsernameAndIsDeletedFalse("admin")).thenReturn(true);
    when(userRepository.count()).thenReturn(0L);

    assertThatThrownBy(() -> userService.register("admin", "admin@example.com", "password123"))
        .isInstanceOf(ConflictException.class);
  }
}
