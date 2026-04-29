package io.vacivor.chert.server.interfaces.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vacivor.chert.server.interfaces.dto.user.UserCreateRequest;
import io.vacivor.chert.server.interfaces.dto.user.UserLoginRequest;
import io.vacivor.chert.server.interfaces.dto.user.UserRegisterRequest;
import io.vacivor.chert.server.interfaces.dto.user.UserUpdateRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthControllerIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldRegisterLoginAndLoadCurrentUser() throws Exception {
    MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                new UserRegisterRequest("admin", "admin@example.com", "password123"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.email").value("admin@example.com"))
        .andExpect(jsonPath("$.roles[?(@ == 'SUPER_ADMIN')]").exists())
        .andReturn();

    HttpSession session = registerResult.getRequest().getSession(false);

    mockMvc.perform(get("/api/auth/me").session((MockHttpSession) session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.email").value("admin@example.com"))
        .andExpect(jsonPath("$.permissions[?(@ == 'rbac:manage')]").exists());
  }

  @Test
  void shouldAllowAdminToManageUsersAndUserToLogin() throws Exception {
    MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                new UserRegisterRequest("admin", "admin@example.com", "password123"))))
        .andExpect(status().isCreated())
        .andReturn();

    MockHttpSession adminSession = (MockHttpSession) registerResult.getRequest().getSession(false);

    mockMvc.perform(post("/api/console/users")
            .session(adminSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                new UserCreateRequest("developer", "developer@example.com", "password123", Set.of("USER")))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("developer"))
        .andExpect(jsonPath("$.email").value("developer@example.com"))
        .andExpect(jsonPath("$.roles[0]").value("USER"));

    mockMvc.perform(patch("/api/console/users/2")
            .session(adminSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                new UserUpdateRequest("newpassword123", Set.of("USER")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("developer"));

    MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                new UserLoginRequest("developer", "newpassword123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("developer"))
        .andReturn();

    mockMvc.perform(get("/api/auth/me").session((MockHttpSession) loginResult.getRequest().getSession(false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("developer"))
        .andExpect(jsonPath("$.email").value("developer@example.com"))
        .andExpect(jsonPath("$.roles[0]").value("USER"));
  }
}
