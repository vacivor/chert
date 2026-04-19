package io.vacivor.chert.server.domain.config;

import io.vacivor.chert.server.common.ConfigFormat;
import io.vacivor.chert.server.common.ConfigType;
import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "config_resource")
@SQLDelete(sql = "update config_resource set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class ConfigResource extends BaseEntity {

  @Column(name = "application_id")
  private Long applicationId;
  @Column(name = "name")
  private String name;
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private ConfigType type;
  @Column(name = "format")
  @Enumerated(EnumType.STRING)
  private ConfigFormat format;
  @Column(name = "version")
  private Long version;
  @Column(name = "description")
  private String description;

  public Long getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(Long applicationId) {
    this.applicationId = applicationId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ConfigType getType() {
    return type;
  }

  public void setType(ConfigType type) {
    this.type = type;
  }

  public ConfigFormat getFormat() {
    return format;
  }

  public void setFormat(ConfigFormat format) {
    this.format = format;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
