package io.vacivor.chert.server.error;

import java.util.Collections;
import java.util.List;

public class ValidationException extends ChertException {

  private final List<ErrorDetail> errors;

  public ValidationException(ErrorCode errorCode, String field, String message) {
    this(errorCode, List.of(new ErrorDetail(field, errorCode.getCode(), message)));
  }

  public ValidationException(ErrorCode errorCode, List<ErrorDetail> errors) {
    super(errorCode, errors.isEmpty() ? errorCode.getMessage() : errors.get(0).message());
    this.errors = Collections.unmodifiableList(errors);
  }

  public List<ErrorDetail> getErrors() {
    return errors;
  }
}
