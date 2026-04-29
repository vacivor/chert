package io.vacivor.chert.server.interfaces.admin.rbac;

import io.vacivor.chert.server.application.rbac.PermissionService;
import io.vacivor.chert.server.interfaces.dto.rbac.PermissionCreateRequest;
import io.vacivor.chert.server.interfaces.dto.rbac.PermissionResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/permissions")
public class PermissionAdminController {

  private final PermissionService permissionService;

  public PermissionAdminController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @GetMapping
  public ResponseEntity<List<PermissionResponse>> list() {
    return ResponseEntity.ok(permissionService.list().stream().map(PermissionResponse::from).toList());
  }

  @PostMapping
  public ResponseEntity<PermissionResponse> create(@RequestBody PermissionCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(PermissionResponse.from(
        permissionService.create(request.code(), request.name(), request.description())));
  }
}
