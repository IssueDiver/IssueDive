package com.issueDive.repository;

import com.issueDive.entity.Issue;
import com.issueDive.entity.IssueLabel;
import com.issueDive.entity.IssueLabelId;
import com.issueDive.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueLabelRepository extends JpaRepository<IssueLabel, IssueLabelId> {
    boolean existsByIssueAndLabel(Issue issue, Label label);
    void deleteByIssueAndLabel(Issue issue, Label label);
    List<IssueLabel> findAllByIssue(Issue issue);
    void deleteByLabelId(Long labelId);
    long countByLabel(Label label);
}
