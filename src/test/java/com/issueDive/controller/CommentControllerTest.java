package com.issueDive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.dto.*;
import com.issueDive.entity.User;
import com.issueDive.exception.CommentNotFoundException;
import com.issueDive.repository.UserRepository;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.service.CommentService;
import com.issueDive.service.UserService;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest: CommentController와 관련된 웹 계층 빈들만 로드합니다.
 * @AutoConfigureMockMvc: MockMvc를 자동으로 설정하며, Spring Security 필터를 활성화합니다.
 * @WithMockUser: 이 클래스의 모든 테스트에 'test@example.com' 사용자로 로그인한 상태를 전역 적용합니다.
 */
@WithMockUser(username = "test@example.com", roles = "USER")
@AutoConfigureMockMvc
@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- MockitoBean: 테스트 대상 컨트롤러의 의존성을 가짜(Mock) 객체로 주입 ---
    @MockitoBean
    private CommentService commentService;

    // 컨트롤러가 의존하는 UserRepository Mock Bean 추가
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    // Security Filter Chain 구성을 위해 필요한 의존성 Mock Bean 추가
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // 테스트에서 공통으로 사용할 ID 값들
    private final Long issueId = 1L;
    private final Long commentId = 1L;
    private final Long currentUserId = 1L; // @WithMockUser와 @BeforeEach에서 설정된 사용자의 ID

    /**
     * @BeforeEach: 각 테스트 메서드 실행 전에 공통적으로 필요한 설정을 수행합니다.
     */
    @BeforeEach
    void setUp() {
        // given: Mock 유저 정보 설정
        User mockUser = new User();
        mockUser.setId(currentUserId);
        mockUser.setEmail("test@example.com");
        var userResponseDto = new UserResponseDTO(currentUserId, "testuser", "test@example.com");

        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        // 컨트롤러가 호출할 userService.findUserByEmail의 동작을 설정
        Mockito.when(userService.findUserByEmail("test@example.com")).thenReturn(userResponseDto);
    }


    @Test
    @DisplayName("[SUCCESS] GET /issues/{issueId}/comments - 이슈의 모든 댓글 조회")
    void getComments_Success() throws Exception {
        // given: 서비스가 빈 댓글 리스트를 반환하도록 설정
        when(commentService.getTreeByIssue(issueId)).thenReturn(Collections.emptyList());

        // when & then: API 호출 및 응답 검증
        mockMvc.perform(get("/issues/{issueId}/comments", issueId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[SUCCESS] POST /issues/{issueId}/comments - 댓글 생성")
    void createComment_Success() throws Exception {
        // given: 요청 DTO 및 Mock 응답 DTO 생성
        CreateCommentRequest request = new CreateCommentRequest();
        request.setDescription("New Comment");

        CommentResponse response = CommentResponse.builder().id(commentId).description("New Comment").author("testuser").createdAt(LocalDateTime.now()).build();
        // 서비스 메서드가 올바른 사용자 ID(currentUserId)로 호출될 때, 위에서 만든 응답을 반환하도록 설정
        when(commentService.createComment(eq(issueId), any(CreateCommentRequest.class), eq(currentUserId))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/issues/{issueId}/comments", issueId)
                        .with(csrf()) // POST, PATCH, DELETE 등 CSRF 보호가 필요한 요청에 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("New Comment"));
    }

    @Test
    @DisplayName("[SUCCESS] PATCH /issues/{issueId}/comments/{commentId} - 댓글 수정")
    void updateComment_Success() throws Exception {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated Comment");

        CommentResponse response = CommentResponse.builder().id(commentId).description("Updated Comment").build();
        when(commentService.updateComment(eq(issueId), eq(commentId), any(UpdateCommentRequest.class), eq(currentUserId))).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Updated Comment"));
    }

    @Test
    @DisplayName("[SUCCESS] DELETE /issues/{issueId}/comments/{commentId} - 댓글 삭제")
    void deleteComment_Success() throws Exception {
        // given: commentService.deleteComment가 currentUserId로 호출될 때 아무것도 하지 않도록 설정
        doNothing().when(commentService).deleteComment(issueId, commentId, currentUserId);

        // when & then
        mockMvc.perform(delete("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent()); // 204 No Content 상태 코드 확인

        // 서비스의 deleteComment가 정확한 인자들로 호출되었는지 검증
        verify(commentService).deleteComment(issueId, commentId, currentUserId);
    }

    @Test
    @DisplayName("[FAIL] POST /issues/{issueId}/comments - 내용 없이 댓글 생성")
    void createComment_BlankDescription_ReturnsBadRequest() throws Exception {
        // given: 유효성 검증(@NotBlank)에 실패할 요청 생성
        CreateCommentRequest request = new CreateCommentRequest();
        request.setDescription("");

        // when & then
        mockMvc.perform(post("/issues/{issueId}/comments", issueId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400 Bad Request 상태 코드 확인
    }

    @Test
    @DisplayName("[FAIL] PATCH /issues/{issueId}/comments/{commentId} - 존재하지 않는 댓글 수정")
    void updateComment_CommentNotFound_ReturnsNotFound() throws Exception {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setDescription("Updated Comment");
        // 서비스가 CommentNotFoundException을 던지도록 설정
        when(commentService.updateComment(eq(issueId), eq(commentId), any(UpdateCommentRequest.class), eq(currentUserId)))
                .thenThrow(new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(patch("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound()) // 404 Not Found 상태 코드 확인
                .andExpect(jsonPath("$.error.code").value("CommentNotFound"));
    }

    @Test
    @DisplayName("[FAIL] DELETE /issues/{issueId}/comments/{commentId} - 권한 없는 댓글 삭제")
    void deleteComment_NotOwner_ReturnsForbidden() throws Exception {
        // given: 서비스가 SecurityException을 던지도록 설정 (권한 없음을 시뮬레이션)
        doThrow(new SecurityException("댓글 작성자가 아닙니다."))
                .when(commentService).deleteComment(issueId, commentId, currentUserId);

        // when & then
        mockMvc.perform(delete("/issues/{issueId}/comments/{commentId}", issueId, commentId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden()) // 403 Forbidden 상태 코드 확인
                .andExpect(jsonPath("$.error.code").value("Forbidden"));
    }

    @Test
    @DisplayName("[SUCCESS] GET /issues/{issueId}/comments/count - 댓글 개수 조회")
    void countComment_Success() throws Exception {
        // given
        long count = 10L;
        when(commentService.countByIssue(issueId)).thenReturn(new CountCommentResponse(issueId, count));

        // when & then
        mockMvc.perform(get("/issues/{issueId}/comments/count", issueId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.issueId").value(issueId))
                .andExpect(jsonPath("$.data.count").value(count));
    }
}