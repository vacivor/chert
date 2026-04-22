package io.vacivor.chert.server.interfaces.admin.config;

import io.vacivor.chert.server.application.config.ConfigDiffService;
import io.vacivor.chert.server.application.config.ConfigReleaseHistoryService;
import io.vacivor.chert.server.application.config.ConfigReleaseService;
import io.vacivor.chert.server.interfaces.dto.config.ConfigDiffResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseHistoryResponse;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseRequest;
import io.vacivor.chert.server.interfaces.dto.config.ConfigReleaseResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console/config-resources/{resourceId}/environments/{environmentId}/releases")
public class ConfigReleaseAdminController {

  private final ConfigReleaseService configReleaseService;
  private final ConfigReleaseHistoryService configReleaseHistoryService;
  private final ConfigDiffService configDiffService;

  public ConfigReleaseAdminController(
      ConfigReleaseService configReleaseService,
      ConfigReleaseHistoryService configReleaseHistoryService,
      ConfigDiffService configDiffService) {
    this.configReleaseService = configReleaseService;
    this.configReleaseHistoryService = configReleaseHistoryService;
    this.configDiffService = configDiffService;
  }

  @GetMapping("/latest")
  public ResponseEntity<ConfigReleaseResponse> getLatest(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    return configReleaseService.findLatest(resourceId, environmentId)
        .map(ConfigReleaseResponse::from)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<ConfigReleaseResponse> publish(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @RequestBody ConfigReleaseRequest request) {
    return ResponseEntity.ok(ConfigReleaseResponse.from(
        configReleaseService.publish(resourceId, environmentId, request.operator(), request.comment())));
  }

  @PostMapping("/{releaseId}/rollback")
  public ResponseEntity<ConfigReleaseResponse> rollback(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId,
      @PathVariable Long releaseId,
      @RequestBody ConfigReleaseRequest request) {
    return ResponseEntity.ok(ConfigReleaseResponse.from(
        configReleaseService.rollback(releaseId, request.operator(), request.comment())));
  }

  @GetMapping("/history")
  public ResponseEntity<List<ConfigReleaseHistoryResponse>> listHistory(
      @PathVariable Long resourceId,
      @PathVariable Long environmentId) {
    return ResponseEntity.ok(configReleaseHistoryService.list(resourceId, environmentId).stream()
        .map(ConfigReleaseHistoryResponse::from)
        .toList());
  }

  @GetMapping("/diff")
  public ResponseEntity<ConfigDiffResponse> diff(
      @RequestParam Long baseReleaseId,
      @RequestParam Long targetReleaseId) {
    ConfigDiffService.DiffResult result = configDiffService.diffReleases(baseReleaseId, targetReleaseId);
    return ResponseEntity.ok(new ConfigDiffResponse(
        result.oldContent(),
        result.newContent(),
        result.hasChanges()));
  }

}
