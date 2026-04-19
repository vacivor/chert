package io.vacivor.chert.server.error;

public enum ConfigResourceErrorCode implements ErrorCode {

  CONFIG_RESOURCE_NOT_FOUND("CONFIG_RESOURCE_NOT_FOUND", "Config resource not found", 404),
  CONFIG_RESOURCE_NAME_DUPLICATED("CONFIG_RESOURCE_NAME_DUPLICATED", "Config resource name already exists", 409),
  CONFIG_RESOURCE_NAME_REQUIRED("CONFIG_RESOURCE_NAME_REQUIRED", "Config resource name is required", 400),
  CONFIG_RESOURCE_TYPE_REQUIRED("CONFIG_RESOURCE_TYPE_REQUIRED", "Config resource type is required", 400),
  CONFIG_RESOURCE_FORMAT_REQUIRED("CONFIG_RESOURCE_FORMAT_REQUIRED", "Config resource format is required", 400),
  CONFIG_RESOURCE_TYPE_NOT_SUPPORTED("CONFIG_RESOURCE_TYPE_NOT_SUPPORTED", "Config resource type not supported", 400),
  CONFIG_RESOURCE_INVALID_PARAMETER("CONFIG_RESOURCE_INVALID_PARAMETER", "Invalid parameter", 400);

  private final String code;
  private final String message;
  private final int status;

  ConfigResourceErrorCode(String code, String message, int status) {
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
