package io.vacivor.chert.server.error;

public enum ConfigReleaseErrorCode implements ErrorCode {

  CONFIG_RELEASE_NOT_FOUND("CONFIG_RELEASE_NOT_FOUND", "Config release not found", 404),
  CONFIG_RELEASE_VERSION_CONFLICT("CONFIG_RELEASE_VERSION_CONFLICT", "Config release version conflict", 409),
  CONFIG_RELEASE_TARGET_NOT_FOUND("CONFIG_RELEASE_TARGET_NOT_FOUND", "Config release target not found", 404);

  private final String code;
  private final String message;
  private final int status;

  ConfigReleaseErrorCode(String code, String message, int status) {
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
