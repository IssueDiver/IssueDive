package com.example.issueDive.service;


import com.example.issueDive.dto.CommentRequest;
import com.example.issueDive.dto.CommentResponse;
import com.example.issueDive.entity.Comment;
import com.example.issueDive.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public CommentResponse createComment(Long issueId, CommentRequest request, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        Issue issue = IssueRepository.findById(issueId)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        Comment.CommentBuilder commentBuilder = Comment.builder()
                .description(request.getDecription())
                .user(user)
                .issue(issue);

        if(request.getParentId() != null){
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CommentNotFoundException("부모 댓글을 찾을 수 없습니다."));

            if(!parent.getIssue().getId().equals(issueId)){
                throw new CommentNotFoundException("부모 댓글과 같은 이슈의 댓글입니다.");
            }

            commentBuilder.parent(parent);
        }
        Comment comment = commentBuilder.build();

        Comment saved = commentRepository.save(comment);
        return CommentResponse.from(saved);
    }

    @Transactional
    public CommentResponse updateComment(Long id, CommentRequest request, Long userId){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        comment.setDescription(request.getDescription());
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long id, Long userId){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        commentRepository.delete(comment);
    }


}

//private final AssignmentCommentRepository commentRepository;
//private final AssignmentRepository assignmentRepository;
//private final UserRepository userRepository;
//private final AuthorizationUtil authorizationUtil;
//
//댓글 등록
//@Transactional
//public AssignmentCommentResponse createComment(Long assignmentId, AssignmentCommentRequest request, Long userId){
//    Users user = userRepository.findById(userId)
//            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. (ID: " + userId + ")")); // 수정
//    Assignment assignment = assignmentRepository.findById(assignmentId)
//            .orElseThrow(() -> new AssignmentNotFoundException("게시글을 찾을 수 없습니다. (ID: " + assignmentId + ")"));
//
//    AssignmentComment.AssignmentCommentBuilder commentBuilder = AssignmentComment.builder()
//            .content(request.getContent())
//            .user(user)
//            .assignment(assignment);
//
//    // 대댓글인 경우 부모 댓글 설정
//    if (request.getParentId() != null) {
//        AssignmentComment parent = commentRepository.findById(request.getParentId())
//                .orElseThrow(() -> new AssignmentCommentNotFoundException("대댓글의 부모 댓글을 찾을 수 없습니다. (ID: " + request.getParentId() + ")"));
//
//        // 부모 댓글이 같은 게시글의 댓글인지 확인
//        if (!parent.getAssignment().getId().equals(assignmentId)) {
//            throw new AssignmentCommentNotFoundException("부모 댓글이 다른 과제에 속해있습니다.");
//        }
//
//        commentBuilder.parent(parent);
//    }
//
//    AssignmentComment comment = commentBuilder.build();
//
//    AssignmentComment saved = commentRepository.save(comment);
//    return AssignmentCommentResponse.from(saved);
//}
//
////댓글 수정
//@Transactional
//public AssignmentCommentResponse updateComment(Long id, AssignmentCommentRequest request, Long userId){
//    AssignmentComment comment = commentRepository.findById(id)
//            .orElseThrow(() -> new AssignmentCommentNotFoundException("댓글을 찾을 수 없습니다."));
//
//    authorizationUtil.checkOwnerOrAdmin(comment.getUser().getId());
//
//    comment.setContent(request.getContent());
//
//    return AssignmentCommentResponse.from(comment);
//}
//
////댓글 삭제
//@Transactional
//public void deleteComment(Long id, Long userId){
//    AssignmentComment comment = commentRepository.findById(id)
//            .orElseThrow(() -> new AssignmentCommentNotFoundException("댓글을 찾을 수 없습니다."));
//
//    authorizationUtil.checkOwnerOrAdmin(comment.getUser().getId());
//
//    commentRepository.delete(comment);
