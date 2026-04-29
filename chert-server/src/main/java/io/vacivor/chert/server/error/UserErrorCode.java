package io.vacivor.chert.server.error;

public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND("USER_NOT_FOUND", "User not found", 404),
  USER_USERNAME_REQUIRED("USER_USERNAME_REQUIRED", "Username is required", 400),
  USER_INVALID_USERNAME("USER_INVALID_USERNAME", "Username is invalid", 400),
  USER_USERNAME_DUPLICATED("USER_USERNAME_DUPLICATED", "Username already exists", 409),
  USER_EMAIL_REQUIRED("USER_EMAIL_REQUIRED", "Email is required", 400),
  USER_INVALID_EMAIL("USER_INVALID_EMAIL", "Email is invalid", 400),
  USER_EMAIL_DUPLICATED("USER_EMAIL_DUPLICATED", "Email already exists", 409),
  USER_ROLE_REQUIRED("USER_ROLE_REQUIRED", "User roles are required", 400),
  USER_PASSWORD_REQUIRED("USER_PASSWORD_REQUIRED", "Password is required", 400),
  USER_INVALID_PASSWORD("USER_INVALID_PASSWORD", "Password is invalid", 400);

  private final String code;
  private final String message;
  private final int status;

  UserErrorCode(String code, String message, int status) {
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
