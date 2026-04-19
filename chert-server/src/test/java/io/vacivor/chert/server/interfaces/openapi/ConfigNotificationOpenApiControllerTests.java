package io.vacivor.chert.server.interfaces.openapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.vacivor.chert.server.application.app.ApplicationSecretService;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.app.ApplicationSecret;
import io.vacivor.chert.server.interfaces.ChertExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.async.DeferredResult;

@ExtendWith(MockitoExtension.class)
class ConfigNotificationOpenApiControllerTests {

  private MockMvc mockMvc;

  @Mock
  private ConfigNotificationService configNotificationService;
  @Mock
  private ApplicationService applicationService;
  @Mock
  private ApplicationSecretService applicationSecretService;

  @InjectMocks
  private ConfigNotificationOpenApiController configNotificationOpenApiController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(configNotificationOpenApiController)
        .setControllerAdvice(new ChertExceptionHandler())
        .build();
  }

  @Test
  void shouldNotifyWatchersWhenConfigReleased() throws Exception {
    Application app = new Application();
    app.setId(1L);
    app.setAppId("test-app");

    ApplicationSecret secret = new ApplicationSecret();
    secret.setId(1L);
    secret.setAppId(1L);
    secret.setAccessKey("valid-key");
    secret.setSecretKey("valid-secret");

    when(applicationService.getByAppId("test-app")).thenReturn(app);
    when(applicationSecretService.findActiveByAccessKey("valid-key")).thenReturn(secret);

    DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();
    when(configNotificationService.watch(eq("test-app"), eq("dev"), any())).thenReturn(deferredResult);

    // 1. 发起长轮询请求
    MvcResult mvcResult = mockMvc.perform(get("/api/open/notifications")
            .header("X-Chert-Access-Key", "valid-key")
            .header("X-Chert-Secret-Key", "valid-secret")
            .param("appId", "test-app")
            .param("env", "dev")
            .param("configName", "app.yml"))
        .andReturn();

    // 2. 模拟通知触发
    deferredResult.setResult(ResponseEntity.ok("app.yml"));

    // 3. 验证长轮询结果
    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(content().string("app.yml"));
  }
}
