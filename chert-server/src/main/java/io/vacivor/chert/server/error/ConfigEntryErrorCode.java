package io.vacivor.chert.server.error;

public enum ConfigEntryErrorCode implements ErrorCode {

  CONFIG_ENTRY_NOT_FOUND("CONFIG_ENTRY_NOT_FOUND", "Config entry not found", 404),
  CONFIG_ENTRY_KEY_DUPLICATED("CONFIG_ENTRY_KEY_DUPLICATED", "Config entry key already exists", 409),
  CONFIG_ENTRY_RESOURCE_ID_REQUIRED("CONFIG_ENTRY_RESOURCE_ID_REQUIRED", "Resource id is required", 400),
  CONFIG_ENTRY_ENVIRONMENT_ID_REQUIRED("CONFIG_ENTRY_ENVIRONMENT_ID_REQUIRED", "Environment id is required", 400),
  CONFIG_ENTRY_KEY_REQUIRED("CONFIG_ENTRY_KEY_REQUIRED", "Key is required", 400),
  CONFIG_ENTRY_VALUE_REQUIRED("CONFIG_ENTRY_VALUE_REQUIRED", "Value is required", 400),
  CONFIG_ENTRY_KEY_INVALID("CONFIG_ENTRY_KEY_INVALID", "Config entry key is invalid", 400),
  CONFIG_ENTRY_INVALID_PARAMETER("CONFIG_ENTRY_INVALID_PARAMETER", "Invalid parameter", 400);

  private final String code;
  private final String message;
  private final int status;

  ConfigEntryErrorCode(String code, String message, int status) {
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
