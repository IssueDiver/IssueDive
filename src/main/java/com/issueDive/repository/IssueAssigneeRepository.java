package com.issueDive.repository;

import com.issueDive.entity.IssueAssignee;
import com.issueDive.entity.IssueAssigneeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueAssigneeRepository extends JpaRepository<IssueAssignee, IssueAssigneeId> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM IssueAssignee ia WHERE ia.issue.id = :issueId")
    void deleteByIssueId(@Param("issueId") Long issueId);
}
