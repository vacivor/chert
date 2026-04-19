package io.vacivor.chert.server.domain.environment;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "environment")
@SQLDelete(sql = "update environment set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class Environment extends BaseEntity {

  @Column(name = "code")
  private String code;
  @Column(name = "name")
  private String name;
  @Column(name = "description")
  private String description;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
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
    return "Environment{" +
        "code='" + code + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        "} " + super.toString();
  }
}
