package io.vacivor.chert.server.domain.app;

import io.vacivor.chert.server.domain.BaseEntity;
import io.vacivor.chert.server.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
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

  @ManyToOne
  @JoinColumn(name = "owner_user_id")
  private User owner;

  @ManyToOne
  @JoinColumn(name = "maintainer_user_id")
  private User maintainer;

  @ManyToMany
  @JoinTable(
      name = "application_developer",
      joinColumns = @JoinColumn(name = "application_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private Set<User> developers = new LinkedHashSet<>();

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

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public User getMaintainer() {
    return maintainer;
  }

  public void setMaintainer(User maintainer) {
    this.maintainer = maintainer;
  }

  public Set<User> getDevelopers() {
    return developers;
  }

  public void setDevelopers(Set<User> developers) {
    this.developers = developers;
  }

  @Override
  public String toString() {
    return "Application{" +
        "appId='" + appId + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", owner=" + (owner != null ? owner.getId() : null) +
        ", maintainer=" + (maintainer != null ? maintainer.getId() : null) +
        ", developers=" + developers.size() +
        "} " + super.toString();
  }
}
