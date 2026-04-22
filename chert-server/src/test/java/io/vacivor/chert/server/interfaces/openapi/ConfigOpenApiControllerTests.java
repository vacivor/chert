package io.vacivor.chert.server.interfaces.openapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.vacivor.chert.server.application.app.ApplicationSecretService;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.application.config.ConfigEntryService;
import io.vacivor.chert.server.application.config.ConfigReleaseService;
import io.vacivor.chert.server.application.config.ConfigResourceService;
import io.vacivor.chert.server.application.environment.EnvironmentService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.app.ApplicationSecret;
import io.vacivor.chert.server.domain.config.ConfigEntry;
import io.vacivor.chert.server.domain.config.ConfigRelease;
import io.vacivor.chert.server.domain.config.ConfigResource;
import io.vacivor.chert.server.domain.environment.Environment;
import io.vacivor.chert.server.interfaces.ChertExceptionHandler;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.async.DeferredResult;

@ExtendWith(MockitoExtension.class)
class ConfigOpenApiControllerTests {

  private MockMvc mockMvc;

  @Mock
  private ApplicationService applicationService;
  @Mock
  private EnvironmentService environmentService;
  @Mock
  private ConfigResourceService configResourceService;
  @Mock
  private ConfigReleaseService configReleaseService;
  @Mock
  private ConfigEntryService configEntryService;
  @Mock
  private ApplicationSecretService applicationSecretService;

  @InjectMocks
  private ConfigOpenApiController configOpenApiController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(configOpenApiController)
        .setControllerAdvice(new ChertExceptionHandler())
        .build();
  }

  @Test
  void shouldFetchConfigWithValidAuth() throws Exception {
    Application app = new Application();
    app.setId(1L);
    app.setAppId("test-app");

    Environment env = new Environment();
    env.setId(1L);
    env.setCode("dev");

    ConfigResource res = new ConfigResource();
    res.setId(1L);
    res.setName("app.yml");
    res.setType(io.vacivor.chert.server.common.ConfigType.CONTENT);
    res.setFormat(io.vacivor.chert.server.common.ConfigFormat.YAML);

    ConfigRelease release = new ConfigRelease();
    release.setSnapshot("key: value");
    release.setUpdatedAt(Instant.now());

    ApplicationSecret secret = new ApplicationSecret();
    secret.setId(1L);
    secret.setAppId(1L);
    secret.setAccessKey("valid-key");
    secret.setSecretKey("valid-secret");

    when(applicationService.getByAppId("test-app")).thenReturn(app);
    when(environmentService.list()).thenReturn(Collections.singletonList(env));
    when(configResourceService.getByApplicationAndName(1L, "app.yml")).thenReturn(res);
    when(configReleaseService.findLatest(1L, 1L)).thenReturn(Optional.of(release));
    when(applicationSecretService.findActiveByAccessKey("valid-key")).thenReturn(secret);

    mockMvc.perform(get("/api/open/configs/test-app/dev/app.yml")
            .header("X-Chert-Access-Key", "valid-key")
            .header("X-Chert-Secret-Key", "valid-secret"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("key: value"));
  }

  @Test
  void shouldReturn401WhenAuthIsMissing() throws Exception {
    Application app = new Application();
    app.setId(1L);
    app.setAppId("test-app");
    when(applicationService.getByAppId("test-app")).thenReturn(app);

    mockMvc.perform(get("/api/open/configs/test-app/dev/app.yml"))
        .andExpect(status().isNotFound());
  }


  @Test
  void shouldUpdateConfigSuccessfully() throws Exception {
    Application app = new Application();
    app.setId(1L);
    app.setAppId("test-app");

    Environment env = new Environment();
    env.setId(1L);
    env.setCode("dev");

    ConfigResource res = new ConfigResource();
    res.setId(1L);
    res.setName("entries.json");
    res.setType(io.vacivor.chert.server.common.ConfigType.ENTRIES);

    ApplicationSecret secret = new ApplicationSecret();
    secret.setId(1L);
    secret.setAppId(1L);
    secret.setAccessKey("valid-key");
    secret.setSecretKey("valid-secret");

    when(applicationService.getByAppId("test-app")).thenReturn(app);
    when(environmentService.list()).thenReturn(Collections.singletonList(env));
    when(configResourceService.getByApplicationAndName(1L, "entries.json")).thenReturn(res);
    when(applicationSecretService.findActiveByAccessKey("valid-key")).thenReturn(secret);

    mockMvc.perform(post("/api/open/configs/test-app/dev/entries.json")
            .header("X-Chert-Access-Key", "valid-key")
            .header("X-Chert-Secret-Key", "valid-secret")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key1\":\"value1\", \"key2\":\"value2\"}"))
        .andExpect(status().isOk());

    verify(configEntryService).replaceEntries(eq(1L), eq(1L), any());
    verify(configReleaseService).publish(eq(1L), eq(1L), any(), any());
  }
}
