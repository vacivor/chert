package io.vacivor.chert.server.interfaces.admin.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.interfaces.ChertExceptionHandler;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationCreateRequest;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ApplicationAdminControllerTests {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private ApplicationService applicationService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ApplicationAdminController(applicationService))
        .setControllerAdvice(new ChertExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnErrorsWhenAppIdIsBlank() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("", "Name", "Desc");

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("appId"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_APP_ID_REQUIRED"));
  }

  @Test
  void shouldReturnErrorsWhenAppIdIsInvalid() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("123app", "Name", "Desc");

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("appId"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_INVALID_APP_ID"));
  }

  @Test
  void shouldReturnErrorsWhenNameIsBlank() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("app", "", "Desc");

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_NAME_REQUIRED"));
  }

  @Test
  void shouldReturnErrorsWhenUpdateNameIsBlank() throws Exception {
    ApplicationUpdateRequest request = new ApplicationUpdateRequest("", "Desc");

    mockMvc.perform(patch("/api/console/applications/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_NAME_REQUIRED"));
  }

  @Test
  void shouldCreateApplicationWhenValid() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("app", "Name", "Desc");
    Application application = new Application();
    application.setAppId("app");
    application.setName("Name");
    
    when(applicationService.create(any())).thenReturn(application);

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.appId").value("app"));
  }
}
