package io.vacivor.chert.server.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "config_release_history")
public class ConfigReleaseHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "config_resource_id")
  private Long configResourceId;
  @Column(name = "environment_id")
  private Long environmentId;
  @Column(name = "release_id")
  private Long releaseId;
  @Column(name = "previous_release_id")
  private Long previousReleaseId;
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Long getReleaseId() {
    return releaseId;
  }

  public void setReleaseId(Long releaseId) {
    this.releaseId = releaseId;
  }

  public Long getPreviousReleaseId() {
    return previousReleaseId;
  }

  public void setPreviousReleaseId(Long previousReleaseId) {
    this.previousReleaseId = previousReleaseId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
