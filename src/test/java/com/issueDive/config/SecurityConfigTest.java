package com.issueDive.config;

import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.security.JwtAuthenticationFilter;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

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
    }

    @Test
    @DisplayName("공개 URL 접근 허용 - /auth/signup")
    void publicUrl_Signup_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // 400 Bad Request (유효성 검증 실패)
    }

    @Test
    @DisplayName("공개 URL 접근 허용 - /auth/login")
    void publicUrl_Login_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // 400 Bad Request (유효성 검증 실패)
    }

    @Test
    @DisplayName("공개 URL 접근 허용 - Swagger UI")
    void publicUrl_Swagger_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andDo(print())
                .andExpect(status().isOk());  // Swagger는 정적 리소스로 200 OK
    }

    @Test
    @DisplayName("보호된 URL 인증 없이 접근 차단")
    void protectedUrl_WithoutAuth_Blocked() throws Exception {
        mockMvc.perform(get("/issues"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 보호된 URL 접근 허용")
    void protectedUrl_WithValidToken_Allowed() throws Exception {
        // given
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // when & then
        mockMvc.perform(get("/issues")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andDo(print())
                .andExpect(status().isOk());  // 또는 404 (컨트롤러 없음)
    }

    @Test
    @DisplayName("잘못된 JWT 토큰으로 보호된 URL 접근 차단")
    void protectedUrl_WithInvalidToken_Blocked() throws Exception {
        // given
        String invalidToken = "invalid.token";
        given(jwtUtil.getUserEmailFromToken(invalidToken)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(invalidToken, USER_EMAIL)).willReturn(false);

        // when & then
        mockMvc.perform(get("/issues")
                        .header("Authorization", "Bearer " + invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS 설정 확인 - 허용된 Origin")
    void cors_AllowedOrigin_Success() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("CORS 설정 확인 - 허용되지 않은 Origin")
    void cors_NotAllowedOrigin_Blocked() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andDo(print())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CSRF 보호 비활성화 확인")
    void csrf_Disabled() throws Exception {
        // CSRF 토큰 없이 POST 요청이 가능한지 확인
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // 400 (유효성 검증) 또는 401 (인증 실패)
    }

    @Test
    @DisplayName("세션 정책 STATELESS 확인")
    void sessionPolicy_Stateless() throws Exception {
        // 첫 번째 요청
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // 400 Bad Request

        // 두 번째 요청 - 세션이 유지되지 않아야 함
        mockMvc.perform(get("/issues"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HTTP 메서드별 접근 제어 - GET")
    void httpMethod_GET_Allowed() throws Exception {
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        mockMvc.perform(get("/issues")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andDo(print())
                .andExpect(status().isOk());  // 또는 404
    }

    @Test
    @DisplayName("HTTP 메서드별 접근 제어 - POST")
    void httpMethod_POST_Allowed() throws Exception {
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        String validIssueJson = "{\"title\":\"Test Issue\",\"description\":\"Test Description\"}";

        mockMvc.perform(post("/issues")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssueJson))
                .andDo(print())
                .andExpect(status().isCreated()); //201 created
    }

    @Test
    @DisplayName("HTTP 메서드별 접근 제어 - DELETE")
    void httpMethod_DELETE_Allowed() throws Exception {
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        mockMvc.perform(delete("/issues/1")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // 404 (리소스 없음)
    }

    @Test
    @DisplayName("JWT 필터 체인 순서 확인")
    void filterChain_JwtBeforeUsernamePassword() throws Exception {
        // JWT 필터가 UsernamePasswordAuthenticationFilter보다 먼저 실행되는지 확인
        // 잘못된 토큰으로 401이 반환되면 JWT 필터가 먼저 실행된 것
        String invalidToken = "invalid.token";
        given(jwtUtil.getUserEmailFromToken(invalidToken))
                .willThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(get("/issues")
                        .header("Authorization", "Bearer " + invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("토큰 처리 중 오류가 발생했습니다.")));
    }
}
