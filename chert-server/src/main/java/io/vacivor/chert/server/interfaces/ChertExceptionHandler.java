package io.vacivor.chert.server.interfaces;

import io.vacivor.chert.server.error.ChertException;
import io.vacivor.chert.server.error.ErrorCode;
import io.vacivor.chert.server.error.ErrorDetail;
import io.vacivor.chert.server.error.ValidationException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChertExceptionHandler {

  @ExceptionHandler(ChertException.class)
  public ResponseEntity<ProblemDetail> handleChertException(ChertException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    HttpStatus status = HttpStatus.valueOf(errorCode.getStatus());
    String detail = ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage();

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setTitle(errorCode.getCode());
    problemDetail.setType(URI.create("urn:chert:error:" + errorCode.getCode()));

    if (ex instanceof ValidationException validationException) {
      problemDetail.setProperty("errors", validationException.getErrors());
    }

    return ResponseEntity.status(status).body(problemDetail);
  }
}
