package io.vacivor.chert.server.interfaces.dto.app;

import io.vacivor.chert.server.domain.user.User;

public record ApplicationUserSummary(
    Long id,
    String username,
    String email
) {

  public static ApplicationUserSummary from(User user) {
    if (user == null) {
      return null;
    }
    return new ApplicationUserSummary(user.getId(), user.getUsername(), user.getEmail());
  }
}
