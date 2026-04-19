package io.vacivor.chert.server.error;

public class BusinessException extends ChertException {

  public BusinessException(ErrorCode errorCode) {
    super(errorCode);
  }

  public BusinessException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
