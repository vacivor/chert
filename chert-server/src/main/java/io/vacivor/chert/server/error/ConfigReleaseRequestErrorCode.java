package io.vacivor.chert.server.error;

public enum ConfigReleaseRequestErrorCode implements ErrorCode {

  CONFIG_RELEASE_REQUEST_NOT_FOUND(
      "CONFIG_RELEASE_REQUEST_NOT_FOUND",
      "Config release request not found",
      404),
  CONFIG_RELEASE_REQUEST_STATUS_INVALID(
      "CONFIG_RELEASE_REQUEST_STATUS_INVALID",
      "Config release request status is invalid",
      409);

  private final String code;
  private final String message;
  private final int status;

  ConfigReleaseRequestErrorCode(String code, String message, int status) {
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
