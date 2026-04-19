package io.vacivor.chert.server.error;

public interface ErrorCode {

  String getCode();

  String getMessage();

  int getStatus();
}
