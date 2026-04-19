package io.vacivor.chert.server.domain.app;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "application_secret")
@SQLDelete(sql = "update application_secret set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class ApplicationSecret extends BaseEntity {

  @Column(name = "app_id")
  private Long appId;
  @Column(name = "status")
  private ApplicationSecretStatusEnum status;
  @Column(name = "name")
  private String name;
  @Column(name = "access_key")
  private String accessKey;
  @Column(name = "secret_key")
  private String secretKey;
  @Column(name = "secret_hint")
  private String secretHint;
  @Column(name = "expires_at")
  private Instant expiresAt;
  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  public Long getAppId() {
    return appId;
  }

  public void setAppId(Long appId) {
    this.appId = appId;
  }

  public ApplicationSecretStatusEnum getStatus() {
    return status;
  }

  public void setStatus(ApplicationSecretStatusEnum status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getSecretHint() {
    return secretHint;
  }

  public void setSecretHint(String secretHint) {
    this.secretHint = secretHint;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  @Override
  public String toString() {
    return "ApplicationSecret{" +
        "appId=" + appId +
        ", status=" + status +
        ", name='" + name + '\'' +
        ", accessKey='" + accessKey + '\'' +
        ", secretKey='" + secretKey + '\'' +
        ", secretHint='" + secretHint + '\'' +
        ", expiresAt=" + expiresAt +
        ", lastUsedAt=" + lastUsedAt +
        "} " + super.toString();
  }
}
