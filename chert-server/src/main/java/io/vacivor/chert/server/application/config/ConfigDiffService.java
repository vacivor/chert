package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.domain.config.ConfigContent;
import io.vacivor.chert.server.domain.config.ConfigRelease;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfigDiffService {

  private final ConfigContentService configContentService;
  private final ConfigReleaseService configReleaseService;

  public ConfigDiffService(
      ConfigContentService configContentService,
      ConfigReleaseService configReleaseService) {
    this.configContentService = configContentService;
    this.configReleaseService = configReleaseService;
  }

  public Optional<DiffResult> diffDraftWithLatestRelease(Long resourceId, Long environmentId) {
    Optional<ConfigContent> draftOpt = configContentService.findLatest(resourceId, environmentId);
    Optional<ConfigRelease> latestReleaseOpt = configReleaseService.findLatest(resourceId, environmentId);

    if (draftOpt.isEmpty() && latestReleaseOpt.isEmpty()) {
      return Optional.empty();
    }

    String oldContent = latestReleaseOpt.map(ConfigRelease::getSnapshot).orElse("");
    String newContent = draftOpt.map(ConfigContent::getContent).orElse("");

    return Optional.of(new DiffResult(oldContent, newContent));
  }

  public DiffResult diffReleases(Long baseReleaseId, Long targetReleaseId) {
    String baseContent = configReleaseService.findById(baseReleaseId)
        .map(ConfigRelease::getSnapshot)
        .orElse("");
    String targetContent = configReleaseService.findById(targetReleaseId)
        .map(ConfigRelease::getSnapshot)
        .orElse("");
    return new DiffResult(baseContent, targetContent);
  }

  public record DiffResult(String oldContent, String newContent) {}
}
