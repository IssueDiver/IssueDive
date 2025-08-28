package com.issueDive.repository;
import com.issueDive.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByIssueIdWithAuthor(Long issueId);

    long countByIssueId(Long issueId);


}
