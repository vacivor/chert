package io.vacivor.chert.server.application.app;

import io.vacivor.chert.server.domain.app.ApplicationSecret;
import io.vacivor.chert.server.domain.app.ApplicationSecretStatusEnum;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.infrastructure.persistence.app.ApplicationSecretRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationSecretService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private final ApplicationSecretRepository applicationSecretRepository;

  public ApplicationSecretService(ApplicationSecretRepository applicationSecretRepository) {
    this.applicationSecretRepository = applicationSecretRepository;
  }

  public List<ApplicationSecret> listByAppId(Long appId) {
    return applicationSecretRepository.findAllByAppId(appId);
  }

  @Transactional
  public ApplicationSecret generate(Long appId, String name) {
    ApplicationSecret secret = new ApplicationSecret();
    secret.setAppId(appId);
    secret.setName(name);
    secret.setStatus(ApplicationSecretStatusEnum.ACTIVE);
    secret.setAccessKey(generateRandomString(16));
    secret.setSecretKey(generateRandomString(32));
    secret.setCreatedAt(Instant.now());
    secret.setUpdatedAt(Instant.now());
    secret.setDeleted(false);
    return applicationSecretRepository.save(secret);
  }

  public ApplicationSecret get(Long id) {
    return applicationSecretRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND,
            "Application secret not found: " + id));
  }

  @Transactional
  public ApplicationSecret updateStatus(Long id, ApplicationSecretStatusEnum status) {
    ApplicationSecret secret = get(id);
    secret.setStatus(status);
    secret.setUpdatedAt(Instant.now());
    return applicationSecretRepository.save(secret);
  }

  @Transactional
  public void delete(Long id) {
    ApplicationSecret secret = get(id);
    applicationSecretRepository.delete(secret);
  }

  public ApplicationSecret findActiveByAccessKey(String accessKey) {
    return applicationSecretRepository.findByAccessKey(accessKey)
        .filter(secret -> secret.getStatus() == ApplicationSecretStatusEnum.ACTIVE)
        .orElseThrow(() -> new NotFoundException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND,
            "Active application secret not found for accessKey: " + accessKey));
  }

  @Transactional
  public void touch(Long id) {
    ApplicationSecret secret = get(id);
    secret.setLastUsedAt(Instant.now());
    applicationSecretRepository.save(secret);
  }

  private String generateRandomString(int length) {
    byte[] bytes = new byte[length];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
  }
}