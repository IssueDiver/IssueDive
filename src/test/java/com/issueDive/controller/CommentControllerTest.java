package com.issueDive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.dto.CommentResponse;
import com.issueDive.dto.CountCommentResponse;
import com.issueDive.dto.CreateCommentRequest;
import com.issueDive.dto.UpdateCommentRequest;
import com.issueDive.exception.CommentNotFoundException;
import com.issueDive.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser
@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private final Long issueId = 1L;
    private final Long userId = 1L;
    private final Long commentId = 1L;

    @Test
    @DisplayName("이슈의 모든 댓글 조회 - 성공")
    void getComments_Success() throws Exception {
        // given
        when(commentService.getTreeByIssue(issueId)).thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/issues/{issueId}/comments", issueId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("댓글 생성 - 성공")
    void createComment_Success() throws Exception {
        // given
        CreateCommentRequest request = new CreateCommentRequest();
        request.setDescription("New Comment");

        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .description("New Comment")
                .author("testuser")
                .createdAt(LocalDateTime.now())
                .build();

        when(commentService.createComment(eq(issueId), any(CreateCommentRequest.class), eq(userId))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/issues/{issueId}/comments", issueId)
                        .with(csrf())
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("New Comment"));
    }

    @Test
    @DisplayName("댓글 수정 - 성공")
    void updateComment_Success() throws Exception {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated Comment");

        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .description("Updated Comment")
                .build();

        when(commentService.updateComment(eq(issueId), eq(commentId), any(UpdateCommentRequest.class), eq(userId))).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf())
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Updated Comment"));
    }

    @Test
    @DisplayName("댓글 삭제 - 성공")
    void deleteComment_Success() throws Exception {
        // given
        // when & then
        mockMvc.perform(delete("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf())
                        .header("X-USER-ID", userId))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(issueId, commentId, userId);
    }

    @Test
    @DisplayName("댓글 생성 - 내용 없음 - 400 Bad Request")
    void createComment_BlankDescription_ReturnsBadRequest() throws Exception {
        // given
        CreateCommentRequest request = new CreateCommentRequest();
        request.setDescription(""); // 유효성 검증에 실패할 내용

        // when & then
        mockMvc.perform(post("/issues/{issueId}/comments", issueId)
                        .with(csrf())
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 수정 - 댓글 없음 - 404 Not Found")
    void updateComment_CommentNotFound_ReturnsNotFound() throws Exception {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated Comment");

        when(commentService.updateComment(eq(issueId), eq(commentId), any(UpdateCommentRequest.class), eq(userId)))
                .thenThrow(new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(patch("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf())
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CommentNotFound"));
    }

    @Test
    @DisplayName("댓글 삭제 - 권한 없음 - 403 Forbidden")
    void deleteComment_NotOwner_ReturnsForbidden() throws Exception {
        // given
        Long notOwnerId = 999L;
        doThrow(new SecurityException("댓글 작성자가 아닙니다."))
                .when(commentService).deleteComment(issueId, commentId, notOwnerId);

        // when & then
        mockMvc.perform(delete("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf())
                        .header("X-USER-ID", notOwnerId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("Forbidden"));
    }

    @Test
    @DisplayName("댓글 개수 조회 - 성공")
    void countComment_Success() throws Exception {
        // given
        long count = 10L;
        when(commentService.countByIssue(issueId)).thenReturn(new CountCommentResponse(issueId, count));

        // when & then
        mockMvc.perform(get("/issues/{issueId}/comments/count", issueId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issueId").value(issueId))
                .andExpect(jsonPath("$.data.count").value(count));
    }
}
