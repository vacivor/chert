package io.vacivor.chert.server.interfaces.admin.user;

import io.vacivor.chert.server.application.user.UserService;
import io.vacivor.chert.server.interfaces.dto.user.UserCreateRequest;
import io.vacivor.chert.server.interfaces.dto.user.UserResponse;
import io.vacivor.chert.server.interfaces.dto.user.UserUpdateRequest;
import io.vacivor.chert.server.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/users")
public class UserAdminController {

  private final UserService userService;

  public UserAdminController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<UserResponse> create(
      @RequestBody UserCreateRequest request,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.create(
        request.username(),
        request.email(),
        request.password(),
        request.roles(),
        authenticatedUser.getUsername())));
  }

  @GetMapping
  public ResponseEntity<Page<UserResponse>> list(Pageable pageable) {
    return ResponseEntity.ok(userService.list(pageable).map(UserResponse::from));
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(UserResponse.from(userService.get(id)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<UserResponse> update(
      @PathVariable Long id,
      @RequestBody UserUpdateRequest request,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return ResponseEntity.ok(UserResponse.from(userService.update(
        id,
        request.password(),
        request.roles(),
        authenticatedUser.getUsername())));
  }
}
