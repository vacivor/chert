package io.vacivor.chert.server.error;

public enum RoleErrorCode implements ErrorCode {

  ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found", 404),
  ROLE_CODE_REQUIRED("ROLE_CODE_REQUIRED", "Role code is required", 400),
  ROLE_INVALID_CODE("ROLE_INVALID_CODE", "Role code is invalid", 400),
  ROLE_CODE_DUPLICATED("ROLE_CODE_DUPLICATED", "Role code already exists", 409);

  private final String code;
  private final String message;
  private final int status;

  RoleErrorCode(String code, String message, int status) {
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
