package io.vacivor.chert.server.interfaces.admin.rbac;

import io.vacivor.chert.server.application.rbac.RoleService;
import io.vacivor.chert.server.interfaces.dto.rbac.RoleCreateRequest;
import io.vacivor.chert.server.interfaces.dto.rbac.RolePermissionUpdateRequest;
import io.vacivor.chert.server.interfaces.dto.rbac.RoleResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/roles")
public class RoleAdminController {

  private final RoleService roleService;

  public RoleAdminController(RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  public ResponseEntity<List<RoleResponse>> list() {
    return ResponseEntity.ok(roleService.list().stream().map(RoleResponse::from).toList());
  }

  @PostMapping
  public ResponseEntity<RoleResponse> create(@RequestBody RoleCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(
        roleService.create(request.code(), request.name(), request.description(), request.permissions())));
  }

  @PatchMapping("/{id}/permissions")
  public ResponseEntity<RoleResponse> updatePermissions(
      @PathVariable Long id,
      @RequestBody RolePermissionUpdateRequest request) {
    return ResponseEntity.ok(RoleResponse.from(roleService.updatePermissions(id, request.permissions())));
  }
}
