package io.vacivor.chert.server.domain.config;

import io.vacivor.chert.server.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "config_release_request")
@SQLDelete(sql = "update config_release_request set is_deleted = true, deleted_at = current_timestamp, updated_at = current_timestamp where id = ?")
@SQLRestriction("is_deleted = false")
public class ConfigReleaseRequest extends BaseEntity {

  @Column(name = "config_resource_id", nullable = false)
  private Long configResourceId;

  @Column(name = "environment_id", nullable = false)
  private Long environmentId;

  @Column(name = "snapshot", columnDefinition = "TEXT", nullable = false)
  private String snapshot;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ConfigReleaseRequestStatus status;

  @Column(name = "request_comment")
  private String requestComment;

  @Column(name = "requested_by", nullable = false)
  private String requestedBy;

  @Column(name = "review_comment")
  private String reviewComment;

  @Column(name = "reviewed_by")
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "approved_release_id")
  private Long approvedReleaseId;

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

  public String getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(String snapshot) {
    this.snapshot = snapshot;
  }

  public ConfigReleaseRequestStatus getStatus() {
    return status;
  }

  public void setStatus(ConfigReleaseRequestStatus status) {
    this.status = status;
  }

  public String getRequestComment() {
    return requestComment;
  }

  public void setRequestComment(String requestComment) {
    this.requestComment = requestComment;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
  }

  public String getReviewComment() {
    return reviewComment;
  }

  public void setReviewComment(String reviewComment) {
    this.reviewComment = reviewComment;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(String reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public Long getApprovedReleaseId() {
    return approvedReleaseId;
  }

  public void setApprovedReleaseId(Long approvedReleaseId) {
    this.approvedReleaseId = approvedReleaseId;
  }
}
