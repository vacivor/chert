package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.error.ConflictException;
import io.vacivor.chert.server.error.ConfigResourceErrorCode;
import io.vacivor.chert.server.error.NotFoundException;
import io.vacivor.chert.server.infrastructure.persistence.config.ConfigResourceRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfigResourceService {

  private final ConfigResourceRepository configResourceRepository;
  private final ApplicationService applicationService;

  public ConfigResourceService(ConfigResourceRepository configResourceRepository,
      ApplicationService applicationService) {
    this.configResourceRepository = configResourceRepository;
    this.applicationService = applicationService;
  }

  @Transactional
  public ConfigResource create(ConfigResource configResource) {
    applicationService.get(configResource.getApplicationId());
    configResourceRepository.findByApplicationIdAndName(configResource.getApplicationId(), configResource.getName())
        .ifPresent(existing -> {
          throw new ConflictException(ConfigResourceErrorCode.CONFIG_RESOURCE_NAME_DUPLICATED,
              "Config resource '" + configResource.getName() + "' already exists in application "
                  + configResource.getApplicationId());
        });
    configResource.setCreatedAt(Instant.now());
    configResource.setUpdatedAt(Instant.now());
    configResource.setDeleted(false);
    return configResourceRepository.save(configResource);
  }

  public ConfigResource get(Long id) {
    return configResourceRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(ConfigResourceErrorCode.CONFIG_RESOURCE_NOT_FOUND,
            "Config resource not found: " + id));
  }

  public ConfigResource getByApplicationAndName(Long applicationId, String name) {
    return configResourceRepository.findByApplicationIdAndName(applicationId, name)
        .orElseThrow(() -> new NotFoundException(ConfigResourceErrorCode.CONFIG_RESOURCE_NOT_FOUND,
            "Config resource not found for application " + applicationId + " and name '" + name + "'"));
  }

  public List<ConfigResource> listByApplication(Long applicationId) {
    applicationService.get(applicationId);
    return configResourceRepository.findByApplicationId(applicationId);
  }

  @Transactional
  public void delete(Long id) {
    if (!configResourceRepository.existsById(id)) {
      throw new NotFoundException(ConfigResourceErrorCode.CONFIG_RESOURCE_NOT_FOUND,
          "Config resource not found: " + id);
    }
    configResourceRepository.deleteById(id);
  }
}
