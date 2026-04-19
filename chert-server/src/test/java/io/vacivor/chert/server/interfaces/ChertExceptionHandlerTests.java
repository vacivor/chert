package io.vacivor.chert.server.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.BusinessException;
import io.vacivor.chert.server.error.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ChertExceptionHandlerTests {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
        .setControllerAdvice(new ChertExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnErrorsInProblemDetail() throws Exception {
    mockMvc.perform(get("/test/error/simple"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("APPLICATION_INVALID_APP_ID"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0].field").value("appId"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_INVALID_APP_ID"))
        .andExpect(jsonPath("$.errors[0].message").value("Invalid appId"));
  }

  @Test
  void shouldNotReturnErrorsInProblemDetailForBusinessError() throws Exception {
    mockMvc.perform(get("/test/error/business"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("APPLICATION_APP_ID_DUPLICATED"))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @Test
  void shouldReturnMultipleErrorsInProblemDetail() throws Exception {
    mockMvc.perform(get("/test/error/multiple"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[0].field").value("field1"))
        .andExpect(jsonPath("$.errors[1].field").value("field2"));
  }

  @RestController
  static class TestController {
    @GetMapping("/test/error/simple")
    public void throwSimpleError() {
      throw new ValidationException(ApplicationErrorCode.APPLICATION_INVALID_APP_ID, "appId", "Invalid appId");
    }

    @GetMapping("/test/error/multiple")
    public void throwMultipleError() {
      throw new ValidationException(io.vacivor.chert.server.error.CommonErrorCode.BAD_REQUEST, java.util.List.of(
          new io.vacivor.chert.server.error.ErrorDetail("field1", "CODE1", "Message 1"),
          new io.vacivor.chert.server.error.ErrorDetail("field2", "CODE2", "Message 2")
      ));
    }

    @GetMapping("/test/error/business")
    public void throwBusinessError() {
      throw new BusinessException(ApplicationErrorCode.APPLICATION_APP_ID_DUPLICATED, "AppId duplicated");
    }
  }
}
