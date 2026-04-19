package io.vacivor.chert.server.domain.config;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "config_content")
@SQLDelete(sql = "update config_content set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class ConfigContent extends BaseEntity {
  @Column(name = "config_resource_id")
  private Long configResourceId;
  @Column(name = "environment_id")
  private Long environmentId;
  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

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


  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "ConfigContent{" +
        "configResourceId=" + configResourceId +
        ", environmentId=" + environmentId +
        ", content='" + content + '\'' +
        "} " + super.toString();
  }
}
