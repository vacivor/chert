package io.vacivor.chert.server.error;

public class NotFoundException extends BusinessException {
  public NotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }

  public NotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
