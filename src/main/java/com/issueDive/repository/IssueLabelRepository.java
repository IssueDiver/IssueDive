package com.issueDive.repository;

import com.issueDive.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssueLabelRepository extends JpaRepository<IssueLabel, IssueLabelId> {
    boolean existsByIssueAndLabel(Issue issue, Label label);
    void deleteByIssueAndLabel(Issue issue, Label label);
    List<IssueLabel> findAllByIssue(Issue issue);

    long countByLabelAndIssue_Status(Label label, IssueStatus issueStatus);

    boolean existsByLabelId(Long labelId);

    @Modifying(clearAutomatically = true)   // db 데이터 변경, 이 쿼리가 실행된 후 영속성 컨텍스트 자동 초기화
    @Query("DELETE FROM IssueLabel il WHERE il.issue.id = :issueId")
    void deleteByIssueId(@Param("issueId") Long issueId);

    @Modifying
    @Query("DELETE FROM IssueLabel il WHERE il.label.id = :labelId")
    void deleteByLabelId(@Param("labelId") Long labelId);

}
