package com.example.issueDive.repository;
import com.example.issueDive.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByIssueIdWithAuthor(Long issueId);

    long countByIssueId(Long issueId);

    boolean existsByIdAndUser_Id(Long commentId, Long userId);
}
