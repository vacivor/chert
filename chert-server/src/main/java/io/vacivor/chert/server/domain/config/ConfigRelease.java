package io.vacivor.chert.server.domain.config;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "config_release")
@SQLDelete(sql = "update config_release set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class ConfigRelease extends BaseEntity {

  @Column(name = "config_resource_id")
  private Long configResourceId;
  @Column(name = "environment_id")
  private Long environmentId;
  @Column(name = "type")
  private String type;
  @Column(name = "snapshot", columnDefinition = "TEXT")
  private String snapshot;
  @Column(name = "version")
  private Long version;
  @Column(name = "comment")
  private String comment;

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(String snapshot) {
    this.snapshot = snapshot;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return "ConfigRelease{" +
        "configResourceId=" + configResourceId +
        ", environmentId=" + environmentId +
        ", type='" + type + '\'' +
        ", version=" + version +
        ", comment='" + comment + '\'' +
        "} " + super.toString();
  }
}
