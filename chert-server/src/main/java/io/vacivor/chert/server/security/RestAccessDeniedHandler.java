package io.vacivor.chert.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vacivor.chert.server.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.valueOf(CommonErrorCode.FORBIDDEN.getStatus()),
        CommonErrorCode.FORBIDDEN.getMessage());
    problemDetail.setTitle(CommonErrorCode.FORBIDDEN.getCode());
    problemDetail.setType(URI.create("urn:chert:error:" + CommonErrorCode.FORBIDDEN.getCode()));

    response.setStatus(CommonErrorCode.FORBIDDEN.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problemDetail);
  }
}
