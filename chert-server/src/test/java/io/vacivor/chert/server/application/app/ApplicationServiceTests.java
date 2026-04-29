package io.vacivor.chert.server.application.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.application.user.UserService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.error.ValidationException;
import io.vacivor.chert.server.infrastructure.persistence.app.ApplicationRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTests {

  @Mock
  private ApplicationRepository applicationRepository;

  @Mock
  private AuditLogService auditLogService;
  @Mock
  private UserService userService;

  @InjectMocks
  private ApplicationService applicationService;

  @Test
  void shouldUseApplicationSpecificCodeWhenAppIdAlreadyExists() {
    Application application = new Application();
    application.setAppId("order-service");
    application.setName("Order Service");
    when(applicationRepository.findByAppIdAndIsDeletedFalse("order-service"))
        .thenReturn(Optional.of(new Application()));

    assertThatThrownBy(() -> applicationService.create(application))
        .isInstanceOf(ConflictException.class)
        .extracting(exception -> ((ConflictException) exception).getErrorCode())
        .isEqualTo(ApplicationErrorCode.APPLICATION_APP_ID_DUPLICATED);

    verify(applicationRepository, never()).save(application);
  }

  @Test
  void shouldUseApplicationSpecificCodeWhenApplicationDoesNotExist() {
    when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> applicationService.get(99L))
        .isInstanceOf(NotFoundException.class)
        .satisfies(exception -> {
          NotFoundException notFoundException = (NotFoundException) exception;
          assertThat(notFoundException.getErrorCode()).isEqualTo(ApplicationErrorCode.APPLICATION_NOT_FOUND);
        });
  }

  @Test
  void shouldPopulateCreatedAtWhenCreatingApplication() {
    Application application = new Application();
    application.setAppId("config-center");
    application.setName("Config Center");
    io.vacivor.chert.server.domain.user.User owner = new io.vacivor.chert.server.domain.user.User();
    owner.setId(1L);
    application.setOwner(owner);
    io.vacivor.chert.server.domain.user.User maintainer = new io.vacivor.chert.server.domain.user.User();
    maintainer.setId(2L);
    application.setMaintainer(maintainer);

    when(applicationRepository.findByAppIdAndIsDeletedFalse("config-center")).thenReturn(Optional.empty());
    when(applicationRepository.save(application)).thenAnswer(invocation -> {
      Application saved = invocation.getArgument(0);
      Instant now = Instant.now();
      saved.setCreatedAt(now);
      saved.setUpdatedAt(now);
      return saved;
    });

    Application created = applicationService.create(application);

    assertThat(created.getCreatedAt()).isNotNull();
    assertThat(created.getUpdatedAt()).isNotNull();
  }

  @Test
  void shouldRejectDeveloperWhenDeveloperMatchesOwner() {
    Application application = new Application();
    application.setAppId("config-center");
    application.setName("Config Center");
    io.vacivor.chert.server.domain.user.User owner = new io.vacivor.chert.server.domain.user.User();
    owner.setId(1L);
    application.setOwner(owner);
    io.vacivor.chert.server.domain.user.User maintainer = new io.vacivor.chert.server.domain.user.User();
    maintainer.setId(2L);
    application.setMaintainer(maintainer);
    application.setDevelopers(new LinkedHashSet<>(java.util.Set.of(owner)));

    when(applicationRepository.findByAppIdAndIsDeletedFalse("config-center")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> applicationService.create(application))
        .isInstanceOf(ValidationException.class)
        .extracting(exception -> ((ValidationException) exception).getErrorCode())
        .isEqualTo(ApplicationErrorCode.APPLICATION_DEVELOPER_CONFLICT);
  }

  @Test
  void shouldRemoveTransferredOwnerFromDevelopers() {
    Application application = new Application();
    application.setId(1L);
    application.setAppId("config-center");
    application.setName("Config Center");
    io.vacivor.chert.server.domain.user.User currentOwner = new io.vacivor.chert.server.domain.user.User();
    currentOwner.setId(1L);
    application.setOwner(currentOwner);
    io.vacivor.chert.server.domain.user.User maintainer = new io.vacivor.chert.server.domain.user.User();
    maintainer.setId(2L);
    application.setMaintainer(maintainer);
    io.vacivor.chert.server.domain.user.User newOwner = new io.vacivor.chert.server.domain.user.User();
    newOwner.setId(3L);
    application.setDevelopers(new LinkedHashSet<>(java.util.Set.of(newOwner)));

    when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
    when(applicationRepository.save(application)).thenAnswer(invocation -> invocation.getArgument(0));

    Application saved = applicationService.transferOwner(1L, newOwner);

    assertThat(saved.getOwner().getId()).isEqualTo(3L);
    assertThat(saved.getDevelopers()).extracting(io.vacivor.chert.server.domain.user.User::getId)
        .doesNotContain(3L);
  }
}
