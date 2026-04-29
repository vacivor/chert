package io.vacivor.chert.server.interfaces.dto.config;

public record ConfigReleasePublishResponse(
    String outcome,
    ConfigReleaseResponse release,
    ConfigReleaseRequestResponse request
) {

  public static ConfigReleasePublishResponse published(ConfigReleaseResponse release) {
    return new ConfigReleasePublishResponse("PUBLISHED", release, null);
  }

  public static ConfigReleasePublishResponse submitted(ConfigReleaseRequestResponse request) {
    return new ConfigReleasePublishResponse("SUBMITTED_FOR_REVIEW", null, request);
  }
}
