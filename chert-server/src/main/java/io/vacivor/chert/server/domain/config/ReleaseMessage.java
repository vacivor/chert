package io.vacivor.chert.server.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "release_message")
public class ReleaseMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "release_id")
  private Long releaseId;
  @Column(name = "config_resource_id")
  private Long configResourceId;
  @Column(name = "environment_id")
  private Long environmentId;
  @Column(name = "app_id", nullable = false)
  private String appId;
  @Column(name = "config_id")
  private String configId;
  @Column(name = "env_code", nullable = false)
  private String envCode;
  @Column(name = "config_name", nullable = false)
  private String name;
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getReleaseId() {
    return releaseId;
  }

  public void setReleaseId(Long releaseId) {
    this.releaseId = releaseId;
  }

  public Long getConfigResourceId() {
    return configResourceId;
  }

  public void setConfigResourceId(Long configResourceId) {
    this.configResourceId = configResourceId;
  }

  public Long getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(Long environmentId) {
    this.environmentId = environmentId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getEnvCode() {
    return envCode;
  }

  public String getConfigId() {
    return configId;
  }

  public void setConfigId(String configId) {
    this.configId = configId;
  }

  public void setEnvCode(String envCode) {
    this.envCode = envCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
