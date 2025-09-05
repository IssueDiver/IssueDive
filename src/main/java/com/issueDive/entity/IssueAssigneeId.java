package com.issueDive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IssueAssigneeId implements Serializable {

    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "user_id")
    private Long userId;
}
