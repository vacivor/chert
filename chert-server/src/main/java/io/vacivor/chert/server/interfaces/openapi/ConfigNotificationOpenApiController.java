package io.vacivor.chert.server.interfaces.openapi;

import io.vacivor.chert.server.application.app.ApplicationSecretService;
import io.vacivor.chert.server.application.app.ApplicationService;
import io.vacivor.chert.server.domain.app.Application;
import io.vacivor.chert.server.domain.app.ApplicationSecret;
import io.vacivor.chert.server.error.ApplicationErrorCode;
import io.vacivor.chert.server.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.List;

@RestController
@RequestMapping("/api/open/notifications")
public class ConfigNotificationOpenApiController {

  private final ConfigNotificationService configNotificationService;
  private final ApplicationService applicationService;
  private final ApplicationSecretService applicationSecretService;

  public ConfigNotificationOpenApiController(
      ConfigNotificationService configNotificationService,
      ApplicationService applicationService,
      ApplicationSecretService applicationSecretService) {
    this.configNotificationService = configNotificationService;
    this.applicationService = applicationService;
    this.applicationSecretService = applicationSecretService;
  }

  @GetMapping
  public DeferredResult<ResponseEntity<ConfigNotificationResponse>> watchNotifications(
      @RequestParam String appId,
      @RequestParam String env,
      @RequestParam(defaultValue = "0") Long lastMessageId,
      @RequestParam(name = "configName") List<String> configNames,
      HttpServletRequest request) {
    Application application = applicationService.getByAppId(appId);
    checkAuth(application, request);
    return configNotificationService.watch(appId, env, configNames, lastMessageId);
  }

  private void checkAuth(Application application, HttpServletRequest request) {
    String accessKey = request.getHeader("X-Chert-Access-Key");
    String secretKey = request.getHeader("X-Chert-Secret-Key");
    if (accessKey == null || accessKey.isEmpty()) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "X-Chert-Access-Key is missing");
    }
    if (secretKey == null || secretKey.isEmpty()) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "X-Chert-Secret-Key is missing");
    }
    ApplicationSecret secret = applicationSecretService.findActiveByAccessKey(accessKey);
    if (!secret.getSecretKey().equals(secretKey)) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "SecretKey does not match");
    }
    if (!secret.getAppId().equals(application.getId())) {
      throw new UnauthorizedException(ApplicationErrorCode.APPLICATION_SECRET_NOT_FOUND, "AccessKey does not match appId");
    }
    applicationSecretService.touch(secret.getId());
  }
}
