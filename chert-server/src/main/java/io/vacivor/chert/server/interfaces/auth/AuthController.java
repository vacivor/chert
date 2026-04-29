package io.vacivor.chert.server.interfaces.auth;

import io.vacivor.chert.server.application.user.UserService;
import io.vacivor.chert.server.domain.user.User;
import io.vacivor.chert.server.error.CommonErrorCode;
import io.vacivor.chert.server.error.UnauthorizedException;
import io.vacivor.chert.server.interfaces.dto.user.UserLoginRequest;
import io.vacivor.chert.server.interfaces.dto.user.UserRegisterRequest;
import io.vacivor.chert.server.interfaces.dto.user.UserResponse;
import io.vacivor.chert.server.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;
  private final UserService userService;

  public AuthController(
      AuthenticationManager authenticationManager,
      SecurityContextRepository securityContextRepository,
      UserService userService) {
    this.authenticationManager = authenticationManager;
    this.securityContextRepository = securityContextRepository;
    this.userService = userService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(
      @RequestBody UserRegisterRequest request,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse) {
    User user = userService.register(request.username(), request.email(), request.password());
    Authentication authentication = authenticate(request.username(), request.password());
    persistAuthentication(authentication, httpServletRequest, httpServletResponse);
    userService.recordSuccessfulLogin(user.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.get(user.getId())));
  }

  @PostMapping("/login")
  public ResponseEntity<UserResponse> login(
      @RequestBody UserLoginRequest request,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse) {
    Authentication authentication = authenticate(request.username(), request.password());
    persistAuthentication(authentication, httpServletRequest, httpServletResponse);
    AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
    userService.recordSuccessfulLogin(principal.getUsername());
    return ResponseEntity.ok(UserResponse.from(userService.get(principal.getId())));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authentication != null && authentication.isAuthenticated()) {
      userService.recordLogout(authentication.getName());
      new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return ResponseEntity.ok(UserResponse.from(userService.get(authenticatedUser.getId())));
  }

  private Authentication authenticate(String username, String password) {
    try {
      return authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(username, password));
    } catch (BadCredentialsException exception) {
      throw new UnauthorizedException(CommonErrorCode.UNAUTHORIZED, "Invalid username or password");
    }
  }

  private void persistAuthentication(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
  }
}
