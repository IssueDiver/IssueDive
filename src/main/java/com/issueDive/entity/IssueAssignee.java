package com.issueDive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "issue_assignee")
@Getter
@Setter
@NoArgsConstructor
public class IssueAssignee {

    @EmbeddedId
    private IssueAssigneeId id = new IssueAssigneeId();

    @MapsId("issueId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public IssueAssignee(Issue issue, User user) {
        this.issue = issue;
        this.user = user;
    }
}
