package io.vacivor.chert.server.interfaces.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.application.app.ApplicationPublishPolicyService;
import io.vacivor.chert.server.application.config.ConfigEntryService;
import io.vacivor.chert.server.application.config.ConfigResourceService;
import io.vacivor.chert.server.application.environment.EnvironmentService;
import io.vacivor.chert.server.application.user.UserService;
import io.vacivor.chert.server.common.ConfigFormat;
import io.vacivor.chert.server.common.ConfigType;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.domain.user.User;
import io.vacivor.chert.server.interfaces.dto.user.UserLoginRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
class ApplicationAccessIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserService userService;

  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private ApplicationPublishPolicyService applicationPublishPolicyService;

  @Autowired
  private EnvironmentService environmentService;

  @Autowired
  private ConfigResourceService configResourceService;

  @Autowired
  private ConfigEntryService configEntryService;

  private User owner;
  private User maintainer;
  private User developer;
  private User outsider;
  private Application application;
  private Environment environment;
  private ConfigResource configResource;

  @BeforeEach
  void setUp() {
    userService.register("admin", "admin@example.com", "password123");
    owner = userService.create("owner", "owner@example.com", "password123", Set.of("USER"), "admin");
    maintainer = userService.create("maintainer", "maintainer@example.com", "password123", Set.of("USER"), "admin");
    developer = userService.create("developer", "developer@example.com", "password123", Set.of("USER"), "admin");
    outsider = userService.create("outsider", "outsider@example.com", "password123", Set.of("USER"), "admin");

    Application createdApplication = new Application();
    createdApplication.setAppId("order-service");
    createdApplication.setName("Order Service");
    createdApplication.setDescription("Order service config");
    createdApplication.setOwner(owner);
    createdApplication.setMaintainer(maintainer);
    createdApplication.setDevelopers(new LinkedHashSet<>(Set.of(developer)));
    application = applicationService.create(createdApplication);

    Environment createdEnvironment = new Environment();
    createdEnvironment.setCode("prod");
    createdEnvironment.setName("Production");
    createdEnvironment.setDescription("Production environment");
    environment = environmentService.create(createdEnvironment);

    ConfigResource createdResource = new ConfigResource();
    createdResource.setApplicationId(application.getId());
    createdResource.setName("application");
    createdResource.setType(ConfigType.ENTRIES);
    createdResource.setFormat(ConfigFormat.NONE);
    createdResource.setVersion(1L);
    createdResource.setDescription("Application entries");
    configResource = configResourceService.create(createdResource);

    ConfigEntry entry = new ConfigEntry();
    entry.setConfigResourceId(configResource.getId());
    entry.setEnvironmentId(environment.getId());
    entry.setKey("feature.enabled");
    entry.setValue("true");
    entry.setValueType("BOOLEAN");
    entry.setDescription("Feature flag");
    configEntryService.save(entry);
  }

  @Test
  void shouldListOnlyAccessibleApplicationsForOrdinaryUsers() throws Exception {
    MockHttpSession developerSession = login("developer", "password123");
    MockHttpSession outsiderSession = login("outsider", "password123");

    mockMvc.perform(get("/api/console/applications").session(developerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].appId").value("order-service"));

    mockMvc.perform(get("/api/console/applications").session(outsiderSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void shouldAllowDeveloperToEditEntriesButBlockPublish() throws Exception {
    MockHttpSession developerSession = login("developer", "password123");

    mockMvc.perform(post("/api/console/config-resources/{resourceId}/environments/{environmentId}/entries",
            configResource.getId(), environment.getId())
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                Map.of(
                    "key", "feature.rollout",
                    "value", "beta",
                    "valueType", "STRING",
                    "description", "Rollout mode"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value("feature.rollout"));

    mockMvc.perform(post("/api/console/config-resources/{resourceId}/environments/{environmentId}/releases",
            configResource.getId(), environment.getId())
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "developer publish"))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.outcome").value("SUBMITTED_FOR_REVIEW"))
        .andExpect(jsonPath("$.request.status").value("PENDING"))
        .andExpect(jsonPath("$.request.requestedBy").value("developer"))
        .andExpect(jsonPath("$.release").isEmpty());
  }

  @Test
  void shouldAllowMaintainerToPublishConfiguration() throws Exception {
    MockHttpSession maintainerSession = login("maintainer", "password123");

    mockMvc.perform(post("/api/console/config-resources/{resourceId}/environments/{environmentId}/releases",
            configResource.getId(), environment.getId())
            .session(maintainerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "approved by maintainer"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("PUBLISHED"))
        .andExpect(jsonPath("$.release.type").value("NORMAL"))
        .andExpect(jsonPath("$.release.version").value(1))
        .andExpect(jsonPath("$.release.comment").value("approved by maintainer"))
        .andExpect(jsonPath("$.request").isEmpty());
  }

  @Test
  void shouldAllowDeveloperToDirectPublishWhenApprovalIsDisabled() throws Exception {
    applicationPublishPolicyService.save(application.getId(), environment.getId(), false);
    MockHttpSession developerSession = login("developer", "password123");

    mockMvc.perform(post("/api/console/config-resources/{resourceId}/environments/{environmentId}/releases",
            configResource.getId(), environment.getId())
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "developer direct publish"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("PUBLISHED"))
        .andExpect(jsonPath("$.release.version").value(1))
        .andExpect(jsonPath("$.release.comment").value("developer direct publish"))
        .andExpect(jsonPath("$.request").isEmpty());
  }

  @Test
  void shouldAllowDeveloperToSubmitReleaseRequestAndMaintainerApprove() throws Exception {
    MockHttpSession developerSession = login("developer", "password123");
    MockHttpSession maintainerSession = login("maintainer", "password123");

    MvcResult submitResult = mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests",
            configResource.getId(), environment.getId())
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "please review this release"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.requestedBy").value("developer"))
        .andExpect(jsonPath("$.requestComment").value("please review this release"))
        .andReturn();

    Long requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests/{requestId}/approve",
            configResource.getId(), environment.getId(), requestId)
            .session(maintainerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "looks good"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.reviewedBy").value("maintainer"))
        .andExpect(jsonPath("$.reviewComment").value("looks good"))
        .andExpect(jsonPath("$.approvedReleaseId").isNumber());

    mockMvc.perform(get(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/latest",
            configResource.getId(), environment.getId())
            .session(maintainerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.comment").value("please review this release"));
  }

  @Test
  void shouldAllowMaintainerToRejectReleaseRequestWithoutPublishing() throws Exception {
    MockHttpSession developerSession = login("developer", "password123");
    MockHttpSession maintainerSession = login("maintainer", "password123");

    MvcResult submitResult = mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests",
            configResource.getId(), environment.getId())
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "please review this release"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();

    Long requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests/{requestId}/reject",
            configResource.getId(), environment.getId(), requestId)
            .session(maintainerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "need more changes"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.reviewedBy").value("maintainer"))
        .andExpect(jsonPath("$.reviewComment").value("need more changes"))
        .andExpect(jsonPath("$.approvedReleaseId").isEmpty());

    mockMvc.perform(get(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/latest",
            configResource.getId(), environment.getId())
            .session(maintainerSession))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldAllowDeveloperToWithdrawPendingReleaseRequest() throws Exception {
    MockHttpSession developerSession = login("developer", "password123");

    MvcResult submitResult = mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests",
            configResource.getId(), environment.getId())
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "please review this release"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();

    Long requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests/{requestId}/withdraw",
            configResource.getId(), environment.getId(), requestId)
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "withdrawing for more changes"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("WITHDRAWN"))
        .andExpect(jsonPath("$.reviewedBy").value("developer"))
        .andExpect(jsonPath("$.reviewComment").value("withdrawing for more changes"));
  }

  @Test
  void shouldAllowDeveloperToResubmitRejectedReleaseRequest() throws Exception {
    MockHttpSession developerSession = login("developer", "password123");
    MockHttpSession maintainerSession = login("maintainer", "password123");

    MvcResult submitResult = mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests",
            configResource.getId(), environment.getId())
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "please review this release"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();

    Long requestId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests/{requestId}/reject",
            configResource.getId(), environment.getId(), requestId)
            .session(maintainerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "need more changes"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));

    mockMvc.perform(post(
            "/api/console/config-resources/{resourceId}/environments/{environmentId}/releases/requests/{requestId}/resubmit",
            configResource.getId(), environment.getId(), requestId)
            .session(developerSession)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("comment", "updated and resubmitted"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.requestedBy").value("developer"))
        .andExpect(jsonPath("$.requestComment").value("updated and resubmitted"));
  }

  private MockHttpSession login(String username, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(new UserLoginRequest(username, password))))
        .andExpect(status().isOk())
        .andReturn();

    HttpSession session = result.getRequest().getSession(false);
    return (MockHttpSession) session;
  }
}
