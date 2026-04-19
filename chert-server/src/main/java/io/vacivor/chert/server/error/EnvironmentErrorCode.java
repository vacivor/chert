package io.vacivor.chert.server.error;

public enum EnvironmentErrorCode implements ErrorCode {

  ENVIRONMENT_NOT_FOUND("ENVIRONMENT_NOT_FOUND", "Environment not found", 404),
  ENVIRONMENT_CODE_DUPLICATED("ENVIRONMENT_CODE_DUPLICATED", "Environment code already exists", 409),
  ENVIRONMENT_CODE_REQUIRED("ENVIRONMENT_CODE_REQUIRED", "Environment code is required", 400),
  ENVIRONMENT_INVALID_CODE("ENVIRONMENT_INVALID_CODE", "Invalid environment code", 400),
  ENVIRONMENT_NAME_REQUIRED("ENVIRONMENT_NAME_REQUIRED", "Environment name is required", 400),
  ENVIRONMENT_INVALID_PARAMETER("ENVIRONMENT_INVALID_PARAMETER", "Invalid parameter", 400);

  private final String code;
  private final String message;
  private final int status;

  EnvironmentErrorCode(String code, String message, int status) {
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
