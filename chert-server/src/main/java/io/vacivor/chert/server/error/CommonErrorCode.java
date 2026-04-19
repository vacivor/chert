package io.vacivor.chert.server.error;

public enum CommonErrorCode implements ErrorCode {

  NOT_FOUND("NOT_FOUND", "Resource not found", 404),
  ALREADY_EXISTS("ALREADY_EXISTS", "Resource already exists", 409),
  BAD_REQUEST("BAD_REQUEST", "Bad request", 400),
  INVALID_PARAMETER("INVALID_PARAMETER", "Invalid parameter", 400),
  UNAUTHORIZED("UNAUTHORIZED", "Unauthorized", 401),
  FORBIDDEN("FORBIDDEN", "Forbidden", 403),
  INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", 500);

  private final String code;
  private final String message;
  private final int status;

  CommonErrorCode(String code, String message, int status) {
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
