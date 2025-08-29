package com.issueDive.service;

import com.issueDive.dto.CommentResponse;
import com.issueDive.dto.CreateCommentRequest;
import com.issueDive.dto.UpdateCommentRequest;
import com.issueDive.entity.Comment;
import com.issueDive.entity.Issue;
import com.issueDive.entity.User;
import com.issueDive.exception.CommentNotFoundException;
import com.issueDive.exception.NotFoundException;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.repository.CommentRepository;
import com.issueDive.repository.IssueRepository;
import com.issueDive.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssueRepository issueRepository;

    @Test
    @DisplayName("댓글 생성 - 성공")
    void createComment_Success() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        CreateCommentRequest request = new CreateCommentRequest();
        request.setDescription("Test Comment");

        User user = User.builder().id(userId).username("testuser").build();
        Issue issue = Issue.builder().id(issueId).build();
        Comment comment = Comment.builder()
                .id(1L)
                .user(user)
                .issue(issue)
                .description("Test Comment")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // when
        CommentResponse response = commentService.createComment(issueId, request, userId);

        // then
        assertThat(response.getDescription()).isEqualTo("Test Comment");
        assertThat(response.getAuthor()).isEqualTo("testuser");
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 - 유저 없음 - 실패")
    void createComment_UserNotFound() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        CreateCommentRequest request = new CreateCommentRequest();
        request.setDescription("Test Comment");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class,
                () -> commentService.createComment(issueId, request, userId));
    }
    
    @Test
    @DisplayName("댓글 수정 - 성공")
    void updateComment_Success() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        Long commentId = 1L;
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated Comment");

        User user = User.builder().id(userId).build();
        Issue issue = Issue.builder().id(issueId).build();
        Comment comment = Comment.builder()
                .id(commentId)
                .user(user)
                .issue(issue)
                .description("Original Comment")
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        // when
        CommentResponse response = commentService.updateComment(issueId, commentId, request, userId);

        // then
        assertThat(response.getDescription()).isEqualTo("Updated Comment");
        assertThat(comment.getDescription()).isEqualTo("Updated Comment");
    }

    @Test
    @DisplayName("댓글 수정 - 댓글 없음 - 실패")
    void updateComment_CommentNotFound() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        Long commentId = 1L;
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated Comment");

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CommentNotFoundException.class,
                () -> commentService.updateComment(issueId, commentId, request, userId));
    }

    @Test
    @DisplayName("댓글 수정 - 작성자 아님 - 실패")
    void updateComment_NotOwner() {
        // given
        Long ownerId = 1L;
        Long requesterId = 2L;
        Long issueId = 1L;
        Long commentId = 1L;
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated Comment");

        User owner = User.builder().id(ownerId).build();
        Issue issue = Issue.builder().id(issueId).build();
        Comment comment = Comment.builder()
                .id(commentId)
                .user(owner)
                .issue(issue)
                .description("Original Comment")
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        // when & then
        assertThrows(SecurityException.class,
                () -> commentService.updateComment(issueId, commentId, request, requesterId));
    }

    @Test
    @DisplayName("댓글 삭제 - 성공")
    void deleteComment_Success() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        Long commentId = 1L;

        User user = User.builder().id(userId).build();
        Issue issue = Issue.builder().id(issueId).build();
        Comment comment = Comment.builder()
                .id(commentId)
                .user(user)
                .issue(issue)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        
        // when
        commentService.deleteComment(issueId, commentId, userId);

        // then
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    @DisplayName("대댓글 생성 - 부모 댓글이 다른 이슈 소속 - 실패(400)")
    void createComment_ParentInDifferentIssue_ThrowsException() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        Long anotherIssueId = 2L;
        Long parentId = 2L;

        CreateCommentRequest request = new CreateCommentRequest();
        request.setDescription("A reply");
        request.setParentId(parentId);

        User user = User.builder().id(userId).build();
        Issue issue = Issue.builder().id(issueId).build();
        Issue anotherIssue = Issue.builder().id(anotherIssueId).build();
        Comment parent = Comment.builder().id(parentId).issue(anotherIssue).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> commentService.createComment(issueId, request, userId));
    }

    @Test
    @DisplayName("댓글 수정 - 이슈 불일치 - 실패(400)")
    void updateComment_MismatchedIssue_ThrowsException() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        Long anotherIssueId = 2L;
        Long commentId = 1L;
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated content");

        User user = User.builder().id(userId).build();
        Issue anotherIssue = Issue.builder().id(anotherIssueId).build();
        Comment comment = Comment.builder().id(commentId).user(user).issue(anotherIssue).build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(Issue.builder().id(issueId).build()));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> commentService.updateComment(issueId, commentId, request, userId));
    }

    @Test
    @DisplayName("댓글 삭제 - 댓글 없음 - 실패(404)")
    void deleteComment_CommentNotFound_ThrowsException() {
        // given
        Long userId = 1L;
        Long issueId = 1L;
        Long commentId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CommentNotFoundException.class,
                () -> commentService.deleteComment(issueId, commentId, userId));
    }

    @Test
    @DisplayName("댓글 삭제 - 작성자 아님 - 실패(403)")
    void deleteComment_NotOwner_ThrowsException() {
        // given
        Long ownerId = 1L;
        Long requesterId = 2L;
        Long issueId = 1L;
        Long commentId = 1L;

        User owner = User.builder().id(ownerId).build();
        Issue issue = Issue.builder().id(issueId).build();
        Comment comment = Comment.builder().id(commentId).user(owner).issue(issue).build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        // when & then
        assertThrows(SecurityException.class,
                () -> commentService.deleteComment(issueId, commentId, requesterId));
    }

    @Test
    @DisplayName("이슈의 댓글 개수 조회 - 성공")
    void countByIssue_Success() {
        // given
        Long issueId = 1L;
        long expectedCount = 5L;
        when(commentRepository.countByIssueId(issueId)).thenReturn(expectedCount);

        // when
        var response = commentService.countByIssue(issueId);

        // then
        assertThat(response.getIssueId()).isEqualTo(issueId);
        assertThat(response.getCount()).isEqualTo(expectedCount);
        verify(commentRepository, times(1)).countByIssueId(issueId);
    }
}
