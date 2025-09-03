package com.issueDive.repository;
import com.issueDive.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.issue.id = :issueId")
    List<Comment> findAllByIssueIdWithUser(@Param("issueId") Long issueId);

    long countByIssueId(Long issueId);


}
