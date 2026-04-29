package io.vacivor.chert.server.security;

import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentAuthenticatedUserService {

  public AuthenticatedUser getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
      throw new UnauthorizedException(CommonErrorCode.UNAUTHORIZED, "Authentication is required");
    }
    return authenticatedUser;
  }
}
