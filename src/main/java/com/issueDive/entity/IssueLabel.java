package com.issueDive.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "issue_label")
public class IssueLabel {

    @Builder.Default
    @EmbeddedId
    private IssueLabelId id = new IssueLabelId();

    @MapsId("issueId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @MapsId("labelId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public IssueLabel(Issue issue, Label label) {
        this.issue = issue;
        this.label = label;
    }

    @PrePersist
    public void prePersist() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }
}
