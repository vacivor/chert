package io.vacivor.chert.server.domain.config;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "config_entry")
@SQLDelete(sql = "update config_entry set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class ConfigEntry extends BaseEntity {

  @Column(name = "config_resource_id")
  private Long configResourceId;
  @Column(name = "environment_id")
  private Long environmentId;
  @Column(name = "entry_key")
  private String key;
  @Column(name = "entry_value")
  private String value;
  @Column(name = "value_type")
  private String valueType;
  @Column(name = "description")
  private String description;

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

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getValueType() {
    return valueType;
  }

  public void setValueType(String valueType) {
    this.valueType = valueType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "ConfigEntry{" +
        "configResourceId=" + configResourceId +
        ", environmentId=" + environmentId +
        ", key='" + key + '\'' +
        ", value='" + value + '\'' +
        ", valueType='" + valueType + '\'' +
        ", description='" + description + '\'' +
        "} " + super.toString();
  }
}
