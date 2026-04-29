package io.vacivor.chert.server.domain.rbac;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "rbac_permission")
@SQLDelete(sql = "update rbac_permission set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class Permission extends BaseEntity {

  @Column(name = "code", nullable = false, length = 128)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "description", length = 255)
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
}
