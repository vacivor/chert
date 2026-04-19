package io.vacivor.chert.server.application.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.application.audit.AuditLogService;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.error.ChertException;
import io.vacivor.chert.server.error.EnvironmentErrorCode;
import io.vacivor.chert.server.infrastructure.persistence.environment.EnvironmentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTests {

  @Mock
  private EnvironmentRepository environmentRepository;

  @Mock
  private AuditLogService auditLogService;

  @InjectMocks
  private EnvironmentService environmentService;

  @Test
  void shouldUseEnvironmentSpecificCodeWhenCodeAlreadyExists() {
    Environment environment = new Environment();
    environment.setCode("prod");
    environment.setName("Production");
    when(environmentRepository.findByCodeAndIsDeletedFalse("prod"))
        .thenReturn(Optional.of(new Environment()));

    assertThatThrownBy(() -> environmentService.create(environment))
        .isInstanceOf(ChertException.class)
        .extracting(exception -> ((ChertException) exception).getErrorCode())
        .isEqualTo(EnvironmentErrorCode.ENVIRONMENT_CODE_DUPLICATED);

    verify(environmentRepository, never()).save(environment);
  }

  @Test
  void shouldUseEnvironmentSpecificCodeWhenEnvironmentDoesNotExist() {
    when(environmentRepository.findById(77L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> environmentService.get(77L))
        .isInstanceOf(ChertException.class)
        .satisfies(exception -> {
          ChertException chertException = (ChertException) exception;
          assertThat(chertException.getErrorCode()).isEqualTo(EnvironmentErrorCode.ENVIRONMENT_NOT_FOUND);
        });
  }
}
