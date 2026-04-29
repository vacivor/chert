package io.vacivor.chert.server.domain.user;

import io.vacivor.chert.server.domain.BaseEntity;
import io.vacivor.chert.server.domain.rbac.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "chert_user")
@SQLDelete(sql = "update chert_user set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

  @Column(name = "username", nullable = false, length = 64)
  private String username;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @ManyToMany
  @JoinTable(
      name = "rbac_user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new LinkedHashSet<>();

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String passwordHash) {
    this.password = passwordHash;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public void setRoles(Set<Role> roles) {
    this.roles = roles;
  }
}
