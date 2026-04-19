package io.vacivor.chert.server.error;

public abstract class ChertException extends RuntimeException {
  private final ErrorCode errorCode;

  protected ChertException(ErrorCode errorCode) {
    this(errorCode, errorCode.getMessage());
  }

  protected ChertException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}