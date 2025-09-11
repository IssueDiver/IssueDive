package com.issueDive.config;

import com.issueDive.dto.IssueResponse;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.service.IssueService;
import com.issueDive.service.TokenBlacklistService;
import com.issueDive.service.UserService;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;


    private static final String API_PREFIX = "/api";

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String USER_EMAIL = "test@example.com";
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username(USER_EMAIL)
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        given(tokenBlacklistService.isBlacklisted(any())).willReturn(false);
    }

    // ===== 공개 엔드포인트 테스트 =====

    @Test
    @DisplayName("공개 URL - /auth/signup 인증 없이 접근 가능")
    void publicUrl_Signup_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(post(API_PREFIX + "/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());// 400 Bad Request (유효성 검증 실패)
    }

    @Test
    @DisplayName("공개 URL - /auth/login 인증 없이 접근 가능")
    void publicUrl_Login_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(post(API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("공개 URL - GET /issues 인증 없이 접근 가능")
    void publicUrl_GetIssues_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(get(API_PREFIX + "/issues"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 URL - GET /labels 인증 없이 접근 가능")
    void publicUrl_GetLabels_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(get(API_PREFIX + "/labels"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ===== 보호된 엔드포인트 테스트 (인증 없이 접근 시 실패) =====

    @Test
    @DisplayName("보호된 URL - POST /issues 인증 없이 접근 차단")
    void protectedUrl_PostIssues_BlockedWithoutAuth() throws Exception {
        mockMvc.perform(post(API_PREFIX + "/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("보호된 URL - DELETE /issues/{id} 인증 없이 접근 차단")
    void protectedUrl_DeleteIssue_BlockedWithoutAuth() throws Exception {
        mockMvc.perform(delete(API_PREFIX + "/issues/1"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("보호된 URL - /auth/logout 인증 없이 접근 차단")
    void protectedUrl_Logout_BlockedWithoutAuth() throws Exception {
        mockMvc.perform(post(API_PREFIX + "/auth/logout"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("보호된 URL - /auth/users/{id} 인증 없이 접근 차단")
    void protectedUrl_GetUser_BlockedWithoutAuth() throws Exception {
        mockMvc.perform(get(API_PREFIX + "/auth/users/1"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ===== JWT 토큰 인증 테스트 =====

    @Test
    @DisplayName("유효한 JWT 토큰으로 보호된 URL 접근 허용")
    void protectedUrl_WithValidToken_Allowed() throws Exception {
        // given
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        UserResponseDTO mockUser = new UserResponseDTO(1L, "Test User", USER_EMAIL);
        given(userService.findUserByEmail(USER_EMAIL)).willReturn(mockUser);

        IssueResponse dummyResponse = new IssueResponse(
                1L, "Test Issue", "Test Description", "OPEN", 1L,
                List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        given(issueService.createIssue(any(), any())).willReturn(dummyResponse);

        String validIssueJson = "{\"title\":\"Test Issue\",\"description\":\"Test Description\"}";

        // when & then
        mockMvc.perform(post(API_PREFIX + "/issues")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssueJson))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 접근 차단")
    void protectedUrl_WithInvalidToken_Blocked() throws Exception {
        // given
        String invalidToken = "invalid.token";
        given(jwtUtil.getUserEmailFromToken(invalidToken)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(invalidToken, USER_EMAIL)).willReturn(false);

        // when & then
        mockMvc.perform(post(API_PREFIX + "/issues")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ===== CORS 설정 테스트 =====

    @Test
    @DisplayName("CORS - 허용된 Origin에서 접근 성공")
    void cors_AllowedOrigin_Success() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("CORS - 허용되지 않은 Origin에서 접근 차단")
    void cors_NotAllowedOrigin_Blocked() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andDo(print())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ===== 세션 정책 테스트 =====

    @Test
    @DisplayName("Stateless 세션 정책 - JSESSIONID 쿠키 생성 안함")
    void sessionPolicy_Stateless_NoSessionCreated() throws Exception {
        mockMvc.perform(post(API_PREFIX + "/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }
}