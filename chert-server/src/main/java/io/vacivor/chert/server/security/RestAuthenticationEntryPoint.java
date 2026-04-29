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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.valueOf(CommonErrorCode.UNAUTHORIZED.getStatus()),
        CommonErrorCode.UNAUTHORIZED.getMessage());
    problemDetail.setTitle(CommonErrorCode.UNAUTHORIZED.getCode());
    problemDetail.setType(URI.create("urn:chert:error:" + CommonErrorCode.UNAUTHORIZED.getCode()));

    response.setStatus(CommonErrorCode.UNAUTHORIZED.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problemDetail);
  }
}
