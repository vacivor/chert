package io.vacivor.chert.server.error;

public enum ApplicationErrorCode implements ErrorCode {

  APPLICATION_NOT_FOUND("APPLICATION_NOT_FOUND", "Application not found", 404),
  APPLICATION_APP_ID_DUPLICATED("APPLICATION_APP_ID_DUPLICATED", "Application appId already exists", 409),
  APPLICATION_APP_ID_REQUIRED("APPLICATION_APP_ID_REQUIRED", "Application appId is required", 400),
  APPLICATION_INVALID_APP_ID("APPLICATION_INVALID_APP_ID", "Invalid application appId", 400),
  APPLICATION_NAME_REQUIRED("APPLICATION_NAME_REQUIRED", "Application name is required", 400),
  APPLICATION_SECRET_NAME_REQUIRED("APPLICATION_SECRET_NAME_REQUIRED", "Application secret name is required", 400),
  APPLICATION_INVALID_PARAMETER("APPLICATION_INVALID_PARAMETER", "Invalid parameter", 400),
  APPLICATION_SECRET_NOT_FOUND("APPLICATION_SECRET_NOT_FOUND", "Application secret not found", 404);

  private final String code;
  private final String message;
  private final int status;

  ApplicationErrorCode(String code, String message, int status) {
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
