package io.vacivor.chert.server.error;

public enum ReleaseMessageErrorCode implements ErrorCode {

  RELEASE_MESSAGE_NOT_FOUND("RELEASE_MESSAGE_NOT_FOUND", "Release message not found", 404),
  RELEASE_MESSAGE_WATCH_INVALID("RELEASE_MESSAGE_WATCH_INVALID", "Release message watch request is invalid", 400);

  private final String code;
  private final String message;
  private final int status;

  ReleaseMessageErrorCode(String code, String message, int status) {
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
