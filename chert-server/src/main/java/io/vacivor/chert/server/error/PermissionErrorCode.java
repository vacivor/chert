package io.vacivor.chert.server.error;

public enum PermissionErrorCode implements ErrorCode {

  PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND", "Permission not found", 404),
  PERMISSION_CODE_REQUIRED("PERMISSION_CODE_REQUIRED", "Permission code is required", 400),
  PERMISSION_INVALID_CODE("PERMISSION_INVALID_CODE", "Permission code is invalid", 400),
  PERMISSION_CODE_DUPLICATED("PERMISSION_CODE_DUPLICATED", "Permission code already exists", 409);

  private final String code;
  private final String message;
  private final int status;

  PermissionErrorCode(String code, String message, int status) {
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
