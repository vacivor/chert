package io.vacivor.chert.server.application.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.domain.app.ApplicationSecret;
import io.vacivor.chert.server.domain.app.ApplicationSecretStatusEnum;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.ChertException;
import io.vacivor.chert.server.infrastructure.persistence.app.ApplicationSecretRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationSecretServiceTests {

  @Mock
  private ApplicationSecretRepository applicationSecretRepository;

  @InjectMocks
  private ApplicationSecretService applicationSecretService;

  @Test
  void shouldListByAppId() {
    Long appId = 1L;
    ApplicationSecret secret = new ApplicationSecret();
    secret.setAppId(appId);
    when(applicationSecretRepository.findAllByAppId(appId)).thenReturn(List.of(secret));

    List<ApplicationSecret> result = applicationSecretService.listByAppId(appId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAppId()).isEqualTo(appId);
  }

  @Test
  void shouldGenerateSecret() {
    Long appId = 1L;
    String name = "test-secret";
    when(applicationSecretRepository.save(any(ApplicationSecret.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ApplicationSecret result = applicationSecretService.generate(appId, name);

    assertThat(result.getAppId()).isEqualTo(appId);
    assertThat(result.getName()).isEqualTo(name);
    assertThat(result.getStatus()).isEqualTo(ApplicationSecretStatusEnum.ACTIVE);
    assertThat(result.getAccessKey()).hasSize(16);
    assertThat(result.getSecretKey()).hasSize(32);
    verify(applicationSecretRepository).save(any(ApplicationSecret.class));
  }

  @Test
  void shouldUpdateStatus() {
    Long id = 1L;
    ApplicationSecret secret = new ApplicationSecret();
    secret.setId(id);
    secret.setStatus(ApplicationSecretStatusEnum.ACTIVE);
    when(applicationSecretRepository.findById(id)).thenReturn(Optional.of(secret));
    when(applicationSecretRepository.save(any(ApplicationSecret.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ApplicationSecret result = applicationSecretService.updateStatus(id, ApplicationSecretStatusEnum.DISABLED);

    assertThat(result.getStatus()).isEqualTo(ApplicationSecretStatusEnum.DISABLED);
    verify(applicationSecretRepository).save(secret);
  }

  @Test
  void shouldThrowExceptionWhenSecretNotFound() {
    Long id = 99L;
    when(applicationSecretRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> applicationSecretService.get(id))
        .isInstanceOf(ChertException.class)
        .hasFieldOrPropertyWithValue("errorCode", ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND);
  }

  @Test
  void shouldFindActiveByAccessKey() {
    String accessKey = "test-access-key";
    ApplicationSecret secret = new ApplicationSecret();
    secret.setAccessKey(accessKey);
    secret.setStatus(ApplicationSecretStatusEnum.ACTIVE);
    when(applicationSecretRepository.findByAccessKey(accessKey)).thenReturn(Optional.of(secret));

    ApplicationSecret result = applicationSecretService.findActiveByAccessKey(accessKey);

    assertThat(result.getAccessKey()).isEqualTo(accessKey);
  }

  @Test
  void shouldThrowWhenFindingInactiveByAccessKey() {
    String accessKey = "test-access-key";
    ApplicationSecret secret = new ApplicationSecret();
    secret.setAccessKey(accessKey);
    secret.setStatus(ApplicationSecretStatusEnum.DISABLED);
    when(applicationSecretRepository.findByAccessKey(accessKey)).thenReturn(Optional.of(secret));

    assertThatThrownBy(() -> applicationSecretService.findActiveByAccessKey(accessKey))
        .isInstanceOf(ChertException.class)
        .hasFieldOrPropertyWithValue("errorCode", ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND);
  }
}