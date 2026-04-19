package io.vacivor.chert.server.domain.app;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "application")
@SQLDelete(sql = "update application set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class Application extends BaseEntity {

  @Column(name = "app_id")
  private String appId;
  @Column(name = "name")
  private String name;
  @Column(name = "description")
  private String description;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "Application{" +
        "appId='" + appId + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        "} " + super.toString();
  }
}
