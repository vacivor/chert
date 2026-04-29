package io.vacivor.chert.server.domain.app;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "application_publish_policy")
@SQLDelete(sql = "update application_publish_policy set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class ApplicationPublishPolicy extends BaseEntity {

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  @Column(name = "environment_id", nullable = false)
  private Long environmentId;

  @Column(name = "publish_requires_approval", nullable = false)
  private Boolean publishRequiresApproval;

  public Long getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(Long applicationId) {
    this.applicationId = applicationId;
  }

  public Long getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(Long environmentId) {
    this.environmentId = environmentId;
  }

  public Boolean getPublishRequiresApproval() {
    return publishRequiresApproval;
  }

  public void setPublishRequiresApproval(Boolean publishRequiresApproval) {
    this.publishRequiresApproval = publishRequiresApproval;
  }
}
