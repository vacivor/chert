package io.vacivor.chert.server.application.app;

import io.vacivor.chert.server.domain.app.ApplicationPublishPolicy;
import io.vacivor.chert.server.infrastructure.persistence.app.ApplicationPublishPolicyRepository;
import io.vacivor.chert.server.application.environment.EnvironmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationPublishPolicyService {

  private static final boolean DEFAULT_PUBLISH_REQUIRES_APPROVAL = true;

  private final ApplicationPublishPolicyRepository applicationPublishPolicyRepository;
  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;

  public ApplicationPublishPolicyService(
      ApplicationPublishPolicyRepository applicationPublishPolicyRepository,
      ApplicationService applicationService,
      EnvironmentService environmentService) {
    this.applicationPublishPolicyRepository = applicationPublishPolicyRepository;
    this.applicationService = applicationService;
    this.environmentService = environmentService;
  }

  public ApplicationPublishPolicy getEffective(Long applicationId, Long environmentId) {
    applicationService.get(applicationId);
    environmentService.get(environmentId);
    return applicationPublishPolicyRepository.findByApplicationIdAndEnvironmentId(applicationId, environmentId)
        .orElseGet(() -> {
          ApplicationPublishPolicy policy = new ApplicationPublishPolicy();
          policy.setApplicationId(applicationId);
          policy.setEnvironmentId(environmentId);
          policy.setPublishRequiresApproval(DEFAULT_PUBLISH_REQUIRES_APPROVAL);
          return policy;
        });
  }

  public boolean publishRequiresApproval(Long applicationId, Long environmentId) {
    return Boolean.TRUE.equals(getEffective(applicationId, environmentId).getPublishRequiresApproval());
  }

  @Transactional
  public ApplicationPublishPolicy save(Long applicationId, Long environmentId, boolean publishRequiresApproval) {
    applicationService.get(applicationId);
    environmentService.get(environmentId);

    ApplicationPublishPolicy policy = applicationPublishPolicyRepository
        .findByApplicationIdAndEnvironmentId(applicationId, environmentId)
        .orElseGet(ApplicationPublishPolicy::new);
    policy.setApplicationId(applicationId);
    policy.setEnvironmentId(environmentId);
    policy.setPublishRequiresApproval(publishRequiresApproval);
    if (policy.getDeleted() == null) {
      policy.setDeleted(false);
    }
    return applicationPublishPolicyRepository.save(policy);
  }
}
