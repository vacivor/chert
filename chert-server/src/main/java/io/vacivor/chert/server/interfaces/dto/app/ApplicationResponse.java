package io.vacivor.chert.server.interfaces.dto.app;

import io.vacivor.chert.server.domain.app.Application;
import java.time.Instant;
import java.util.List;

public record ApplicationResponse(
    Long id,
    String appId,
    String name,
    String description,
    ApplicationUserSummary owner,
    ApplicationUserSummary maintainer,
    List<ApplicationUserSummary> developers,
    Instant createdAt,
    Instant updatedAt) {

  public static ApplicationResponse from(Application application) {
    return new ApplicationResponse(
        application.getId(),
        application.getAppId(),
        application.getName(),
        application.getDescription(),
        ApplicationUserSummary.from(application.getOwner()),
        ApplicationUserSummary.from(application.getMaintainer()),
        application.getDevelopers().stream()
            .map(ApplicationUserSummary::from)
            .sorted(java.util.Comparator.comparing(ApplicationUserSummary::id))
            .toList(),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }
}
