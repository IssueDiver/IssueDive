package com.issueDive.repository;

import com.issueDive.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    @Query("SELECT DISTINCT i FROM Issue i LEFT JOIN FETCH i.issueLabels il LEFT JOIN FETCH il.label WHERE i.id = :id")
    Optional<Issue> findWithLabelsById(@Param("id") Long id);

    @Query("SELECT DISTINCT i FROM Issue i " +
            "LEFT JOIN FETCH i.issueAssignees ia LEFT JOIN FETCH ia.user " +
            "LEFT JOIN FETCH i.issueLabels il LEFT JOIN FETCH il.label " +
            "WHERE i.id = :id")
    Optional<Issue> findWithDetailsById(@Param("id") Long id);

    @Query("SELECT DISTINCT i FROM Issue i " +
            "LEFT JOIN FETCH i.issueAssignees ia LEFT JOIN FETCH ia.user " +
            "LEFT JOIN FETCH i.issueLabels il LEFT JOIN FETCH il.label " +
            "WHERE i.id IN :ids")
    List<Issue> findAllByIdInWithDetails(@Param("ids") List<Long> ids);
}
