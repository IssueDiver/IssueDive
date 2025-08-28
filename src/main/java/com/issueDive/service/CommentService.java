package com.issueDive.service;

import com.issueDive.dto.CommentResponse;
import com.issueDive.dto.*;
import com.issueDive.entity.Comment;
import com.issueDive.entity.Issue;
import com.issueDive.entity.User;
import com.issueDive.repository.CommentRepository;
import com.issueDive.repository.IssueRepository;
import com.issueDive.repository.UserRepository;
import com.issueDive.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;


    @Transactional
    public CommentResponse createComment(Long issueId, CreateCommentRequest request, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("이슈를 찾을 수 없습니다."));

        Comment.CommentBuilder commentBuilder = Comment.builder()
                .description(request.getDescription())
                .user(user)
                .issue(issue);

        if(request.getParentId() != null){
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CommentNotFoundException("부모 댓글을 찾을 수 없습니다."));

            if(!parent.getIssue().getId().equals(issueId)){
                throw new IllegalArgumentException("부모 댓글이 다른 이슈에 소속되어 있습니다.");
            }

            commentBuilder.parent(parent);
        }
        Comment comment = commentBuilder.build();

        Comment saved = commentRepository.save(comment);
        return CommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getTreeByIssue(Long issueId){
        issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("이슈를 찾을 수 없습니다."));

        List<Comment> all = commentRepository.findAllByIssueIdWithAuthor(issueId);
        return CommentResponse.fromAllToTree(all);
    }

    @Transactional
    public CommentResponse updateComment(Long issueId, Long id, UpdateCommentRequest request, Long userId){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));
        issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("이슈를 찾을 수 없습니다."));
        if (!Objects.equals(comment.getIssue().getId(), issueId)) {
            throw new IllegalArgumentException("요청한 이슈와 댓글의 소속이 일치하지 않습니다.");
        }

        // 권한 검증 (403)
        if (!Objects.equals(comment.getUser().getId(), userId)) {
            throw new SecurityException("댓글 작성자가 아닙니다.");
        }

        comment.changeDescription(request.getDescription());
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long issueId, Long id, Long userId){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));
        issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("이슈를 찾을 수 없습니다."));

        // 이슈-댓글 소속 검증 (400)
        if (!Objects.equals(comment.getIssue().getId(), issueId)) {
            throw new IllegalArgumentException("요청한 이슈와 댓글의 소속이 일치하지 않습니다.");
        }

        // 권한 검증 (403)
        if (!Objects.equals(comment.getUser().getId(), userId)) {
            throw new SecurityException("댓글 작성자가 아닙니다.");
        }
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public CountCommentResponse countByIssue(Long issueId){
        long count = commentRepository.countByIssueId(issueId);
        return new CountCommentResponse(issueId, count);
    }


}
