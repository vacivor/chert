package io.vacivor.chert.server.error;

public class ForbiddenException extends BusinessException {

  public ForbiddenException(ErrorCode errorCode) {
    super(errorCode);
  }

  public ForbiddenException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
