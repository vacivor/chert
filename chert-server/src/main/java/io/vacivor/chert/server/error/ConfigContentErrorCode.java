package io.vacivor.chert.server.error;

public enum ConfigContentErrorCode implements ErrorCode {

  CONFIG_CONTENT_NOT_FOUND("CONFIG_CONTENT_NOT_FOUND", "Config content not found", 404),
  CONFIG_CONTENT_RESOURCE_ID_REQUIRED("CONFIG_CONTENT_RESOURCE_ID_REQUIRED", "Resource id is required", 400),
  CONFIG_CONTENT_ENVIRONMENT_ID_REQUIRED("CONFIG_CONTENT_ENVIRONMENT_ID_REQUIRED", "Environment id is required", 400),
  CONFIG_CONTENT_REQUIRED("CONFIG_CONTENT_REQUIRED", "Content is required", 400),
  CONFIG_CONTENT_FORMAT_UNSUPPORTED("CONFIG_CONTENT_FORMAT_UNSUPPORTED", "Config content format is unsupported", 400),
  CONFIG_CONTENT_EMPTY("CONFIG_CONTENT_EMPTY", "Config content is empty", 400);

  private final String code;
  private final String message;
  private final int status;

  ConfigContentErrorCode(String code, String message, int status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public int getStatus() {
    return status;
  }
}
