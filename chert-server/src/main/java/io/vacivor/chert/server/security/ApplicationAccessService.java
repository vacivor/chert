package io.vacivor.chert.server.security;

import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.application.rbac.RbacDefaults;
import io.vacivor.chert.server.application.config.ConfigResourceService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.user.User;
import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.ForbiddenException;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAccessService {

  private final CurrentAuthenticatedUserService currentAuthenticatedUserService;
  private final ApplicationService applicationService;
  private final ConfigResourceService configResourceService;

  public ApplicationAccessService(
      CurrentAuthenticatedUserService currentAuthenticatedUserService,
      ApplicationService applicationService,
      ConfigResourceService configResourceService) {
    this.currentAuthenticatedUserService = currentAuthenticatedUserService;
    this.applicationService = applicationService;
    this.configResourceService = configResourceService;
  }

  public boolean isSuperAdmin() {
    return currentAuthenticatedUserService.getCurrentUser().getAuthorities().stream()
        .anyMatch(authority -> ("ROLE_" + RbacDefaults.ROLE_SUPER_ADMIN).equals(authority.getAuthority()));
  }

  public Long currentUserId() {
    return currentAuthenticatedUserService.getCurrentUser().getId();
  }

  public String currentUsername() {
    return currentAuthenticatedUserService.getCurrentUser().getUsername();
  }

  public void requireApplicationMember(Long applicationId) {
    Application application = applicationService.get(applicationId);
    requireApplicationMember(application);
  }

  public void requireApplicationManager(Long applicationId) {
    Application application = applicationService.get(applicationId);
    requireApplicationManager(application);
  }

  public void requireApplicationOwner(Long applicationId) {
    Application application = applicationService.get(applicationId);
    requireApplicationOwner(application);
  }

  public void requireResourceMember(Long resourceId) {
    requireApplicationMember(configResourceService.get(resourceId).getApplicationId());
  }

  public void requireResourceManager(Long resourceId) {
    requireApplicationManager(configResourceService.get(resourceId).getApplicationId());
  }

  public boolean canManageResource(Long resourceId) {
    return canManage(applicationService.get(configResourceService.get(resourceId).getApplicationId()));
  }

  public boolean canAccess(Application application) {
    if (isSuperAdmin()) {
      return true;
    }
    AuthenticatedUser currentUser = currentAuthenticatedUserService.getCurrentUser();
    Long currentUserId = currentUser.getId();
    return matches(application.getOwner(), currentUserId)
        || matches(application.getMaintainer(), currentUserId)
        || application.getDevelopers().stream().map(User::getId).anyMatch(currentUserId::equals);
  }

  public boolean canManage(Application application) {
    if (isSuperAdmin()) {
      return true;
    }
    AuthenticatedUser currentUser = currentAuthenticatedUserService.getCurrentUser();
    Long currentUserId = currentUser.getId();
    return matches(application.getOwner(), currentUserId)
        || matches(application.getMaintainer(), currentUserId);
  }

  private void requireApplicationMember(Application application) {
    if (!canAccess(application)) {
      throw new ForbiddenException(CommonErrorCode.FORBIDDEN, "You are not a member of this application");
    }
  }

  private void requireApplicationManager(Application application) {
    if (!canManage(application)) {
      throw new ForbiddenException(CommonErrorCode.FORBIDDEN, "Only application owner or maintainer can perform this action");
    }
  }

  private void requireApplicationOwner(Application application) {
    if (isSuperAdmin()) {
      return;
    }
    if (!matches(application.getOwner(), currentUserId())) {
      throw new ForbiddenException(CommonErrorCode.FORBIDDEN, "Only application owner can transfer application ownership");
    }
  }

  private boolean matches(User user, Long userId) {
    return user != null && user.getId() != null && user.getId().equals(userId);
  }
}
