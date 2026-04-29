package io.vacivor.chert.server.security;

import io.vacivor.chert.server.application.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ChertUserDetailsService implements UserDetailsService {

  private final UserService userService;

  public ChertUserDetailsService(UserService userService) {
    this.userService = userService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    try {
      return AuthenticatedUser.from(userService.getByUsername(username));
    } catch (RuntimeException exception) {
      throw new UsernameNotFoundException("User not found: " + username, exception);
    }
  }
}
