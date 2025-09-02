package com.issueDive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.dto.CreateIssueRequest;
import com.issueDive.dto.IssueFilterRequest;
import com.issueDive.dto.IssueResponse;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.entity.User;
import com.issueDive.exception.ErrorCode;
import com.issueDive.exception.NotFoundException;
import com.issueDive.exception.ValidationException;
import com.issueDive.repository.UserRepository;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.service.IssueService;
import com.issueDive.service.UserService;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest: IssueController와 관련된 웹 계층 빈들만 로드합니다.
 * @AutoConfigureMockMvc: MockMvc를 자동으로 설정합니다. (addFilters = false 제거로 Security 필터 활성화)
 * @WithMockUser: 이 클래스의 모든 테스트에 'test@example.com' 사용자로 로그인한 상태를 전역 적용합니다.
 */
@WithMockUser(username = "test@example.com", roles = "USER")
@AutoConfigureMockMvc
@WebMvcTest(IssueController.class)
public class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청 시뮬레이션 객체

    @Autowired
    private ObjectMapper objectMapper; // JSON <-> Java Object 변환 객체

    // --- MockitoBean: 테스트 대상 컨트롤러의 의존성을 가짜(Mock) 객체로 주입 ---
    @MockitoBean
    private IssueService issueService;

    // 컨트롤러의 의존성인 UserRepository를 Mock Bean으로 추가
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserService userService;

    // Security Filter Chain 구성을 위해 필요한 의존성 Mock Bean 추가
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // 테스트 전역에서 사용할 Mock 유저의 ID
    private final Long currentUserId = 1L;

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

        // 컨트롤러에서 @AuthenticationPrincipal을 통해 얻은 이메일로 DB 조회를 시도할 것이므로,
        // userRepository.findByEmail이 호출될 때 위에서 만든 Mock 유저 객체를 반환하도록 설정합니다.
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // 컨트롤러가 호출할 userService.findUserByEmail의 동작을 설정
        Mockito.when(userService.findUserByEmail("test@example.com")).thenReturn(userResponseDto);
    }


    @Test
    @DisplayName("[SUCCESS] POST /issues - 이슈 생성 성공")
    public void createIssue_success() throws Exception {
        // given: 서비스가 반환할 Mock 응답 데이터 생성
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "OPEN", currentUserId, 2L, List.of(1L, 2L), LocalDateTime.now(), LocalDateTime.now());

        // issueService의 createIssue 메서드가 'currentUserId'와 함께 호출될 때, mockResponse를 반환하도록 설정
        Mockito.when(issueService.createIssue(any(CreateIssueRequest.class), eq(currentUserId))).thenReturn(mockResponse);

        String requestBody = """
            {
                "title": "제목",
                "description": "설명",
                "assigneeId": 2
            }
            """;

        // when & then: API를 호출하고 응답을 검증
        mockMvc.perform(post("/issues")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.authorId").value(currentUserId));
    }

    @Test
    @DisplayName("[SUCCESS] GET /issues - 이슈 목록 필터링 및 페이징 조회 성공")
    void getIssues_returnsPagedResults() throws Exception {
        // given: Mock Service가 반환할 Page 객체 생성
        List<IssueResponse> issueList = List.of(
                new IssueResponse(1L, "첫 번째 이슈", "설명 1", "OPEN", 1L, 2L, List.of(), LocalDateTime.now(), LocalDateTime.now())
        );
        Page<IssueResponse> mockPage = new PageImpl<>(issueList, PageRequest.of(0, 10), issueList.size());

        Mockito.when(issueService.getFilteredIssues(any(IssueFilterRequest.class)))
                .thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/issues")
                        .param("status", "OPEN")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("[SUCCESS] GET /issues/{id} - 특정 이슈 조회 성공")
    public void getIssue_success() throws Exception {
        // given
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "OPEN", 1L, 2L, List.of(), LocalDateTime.now(), LocalDateTime.now());
        Mockito.when(issueService.getIssue(1L)).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/issues/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    @DisplayName("[FAIL] GET /issues/{id} - 존재하지 않는 이슈 조회")
    void getIssue_notFound_fail() throws Exception {
        // given
        Mockito.when(issueService.getIssue(999L)).thenThrow(new NotFoundException("Issue not found"));

        // when & then
        mockMvc.perform(get("/issues/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IssueNotFound"));
    }

    @Test
    @DisplayName("[SUCCESS] PATCH /issues/{id}/status - 이슈 상태 변경 성공")
    void changeIssueStatus_success() throws Exception {
        // given
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "CLOSED", 1L, 2L, List.of(), LocalDateTime.now(), LocalDateTime.now());
        Mockito.when(issueService.changeIssueStatus(1L, "CLOSED")).thenReturn(mockResponse);

        String requestBody = """
        {
            "status": "CLOSED"
        }
        """;

        // when & then
        mockMvc.perform(patch("/issues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    @DisplayName("[FAIL] PATCH /issues/{id}/status - 유효하지 않은 상태 값으로 변경")
    void changeIssueStatus_invalidStatus_badRequest() throws Exception {
        // given
        Mockito.when(issueService.changeIssueStatus(1L, "INVALID"))
                .thenThrow(new ValidationException(ErrorCode.InvalidStatus, "status must be either OPEN or CLOSED"));

        String requestBody = """
        {
            "status": "INVALID"
        }
        """;

        // when & then
        mockMvc.perform(patch("/issues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("InvalidStatus"));
    }
}