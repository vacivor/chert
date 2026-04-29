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
import io.vacivor.chert.server.domain.user.User;
import io.vacivor.chert.server.interfaces.ChertExceptionHandler;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationCreateRequest;
import io.vacivor.chert.server.interfaces.dto.app.ApplicationUpdateRequest;
import io.vacivor.chert.server.security.ApplicationAccessService;
import java.util.Set;
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

  @Mock
  private ApplicationAccessService applicationAccessService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(
            new ApplicationAdminController(applicationService, applicationAccessService))
        .setControllerAdvice(new ChertExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnErrorsWhenAppIdIsBlank() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("", "Name", "Desc", 1L, 2L, Set.of());

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("appId"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_APP_ID_REQUIRED"));
  }

  @Test
  void shouldReturnErrorsWhenAppIdIsInvalid() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("123app", "Name", "Desc", 1L, 2L, Set.of());

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("appId"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_INVALID_APP_ID"));
  }

  @Test
  void shouldReturnErrorsWhenNameIsBlank() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("app", "", "Desc", 1L, 2L, Set.of());

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_NAME_REQUIRED"));
  }

  @Test
  void shouldReturnErrorsWhenUpdateNameIsBlank() throws Exception {
    ApplicationUpdateRequest request = new ApplicationUpdateRequest("", "Desc", null, null, null);

    mockMvc.perform(patch("/api/console/applications/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_NAME_REQUIRED"));
  }

  @Test
  void shouldRejectOwnerUpdateInRegularPatch() throws Exception {
    ApplicationUpdateRequest request = new ApplicationUpdateRequest("Name", "Desc", 99L, null, null);

    mockMvc.perform(patch("/api/console/applications/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("ownerUserId"))
        .andExpect(jsonPath("$.errors[0].code").value("APPLICATION_OWNER_UPDATE_FORBIDDEN"));
  }

  @Test
  void shouldCreateApplicationWhenValid() throws Exception {
    ApplicationCreateRequest request = new ApplicationCreateRequest("app", "Name", "Desc", 1L, 2L, Set.of(3L, 4L));
    Application application = new Application();
    application.setAppId("app");
    application.setName("Name");
    User owner = new User();
    owner.setId(1L);
    owner.setUsername("owner");
    owner.setEmail("owner@example.com");
    application.setOwner(owner);
    User maintainer = new User();
    maintainer.setId(2L);
    maintainer.setUsername("maintainer");
    maintainer.setEmail("maintainer@example.com");
    application.setMaintainer(maintainer);
    
    when(applicationService.resolveUser(1L, "ownerUserId")).thenReturn(owner);
    when(applicationService.resolveUser(2L, "maintainerUserId")).thenReturn(maintainer);
    when(applicationService.resolveUsers(Set.of(3L, 4L))).thenReturn(java.util.Set.of());
    when(applicationService.create(any())).thenReturn(application);

    mockMvc.perform(post("/api/console/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.appId").value("app"))
        .andExpect(jsonPath("$.owner.id").value(1L))
        .andExpect(jsonPath("$.maintainer.id").value(2L));
  }
}
