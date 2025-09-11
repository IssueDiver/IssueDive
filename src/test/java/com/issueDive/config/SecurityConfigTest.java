package com.issueDive.config;

import com.issueDive.dto.CountCommentResponse;
import com.issueDive.dto.IssueNavigationResponse;
import com.issueDive.dto.IssueResponse;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.security.JwtAuthenticationFilter;
import com.issueDive.service.CommentService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    // 555 변경: CommentService MockBean 추가
    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean  // 이미 있을 것
    private TokenBlacklistService tokenBlacklistService;

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

        when(tokenBlacklistService.isBlacklisted(any())).thenReturn(false);

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

    // 555 변경: /auth/refresh 공개 엔드포인트 테스트 추가
    @Test
    @DisplayName("공개 URL 접근 허용 - /auth/refresh")
    void publicUrl_Refresh_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some.refresh.token\"}"))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // 400 or 401 (토큰 검증 실패)
    }

    // 555 변경: /auth/logout은 인증이 필요한 엔드포인트 테스트 추가
    @Test
    @DisplayName("보호된 URL - /auth/logout은 인증 필요")
    void protectedUrl_Logout_RequiresAuth() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andDo(print())
                .andExpect(status().isForbidden());  // 인증 없이 접근 시 403
    }

    // 555 변경: /auth/users/{id}는 인증이 필요한 엔드포인트 테스트 추가
    @Test
    @DisplayName("보호된 URL - /auth/users/{id}는 인증 필요")
    void protectedUrl_GetUser_RequiresAuth() throws Exception {
        mockMvc.perform(get("/auth/users/1"))
                .andDo(print())
                .andExpect(status().isForbidden());  // 인증 없이 접근 시 403
    }

    // 555 변경: /auth/users는 인증이 필요한 엔드포인트 테스트 추가
    @Test
    @DisplayName("보호된 URL - /auth/users는 인증 필요")
    void protectedUrl_GetAllUsers_RequiresAuth() throws Exception {
        mockMvc.perform(get("/auth/users"))
                .andDo(print())
                .andExpect(status().isForbidden());  // 인증 없이 접근 시 403
    }

    @Test
    @DisplayName("공개 URL 접근 허용 - Swagger UI")
    void publicUrl_Swagger_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andDo(print())
                .andExpect(status().isOk());  // Swagger는 정적 리소스로 200 OK
    }

    @Test
    @DisplayName("permitAll()된 GET /issues는 인증 없이 접근 가능")
    void publicGetIssues_WithoutAuth_Allowed() throws Exception {
        // GET /issues는 이제 permitAll이므로 403 Forbidden이 아닌 200 OK를 기대해야 합니다.
        mockMvc.perform(get("/issues"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // 555 변경: GET /issues/{id}/comments 공개 엔드포인트 테스트 추가 (Mock 데이터 설정)
    @Test
    @DisplayName("공개 URL - GET /issues/{id}/comments는 인증 없이 접근 가능")
    void publicGetComments_WithoutAuth_Allowed() throws Exception {
        // Mock 데이터 설정
        given(commentService.getTreeByIssue(1L)).willReturn(new ArrayList<>());

        mockMvc.perform(get("/issues/1/comments"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // 555 변경: GET /issues/{id}/comments/count 공개 엔드포인트 테스트 추가 (Mock 데이터 설정)
    @Test
    @DisplayName("공개 URL - GET /issues/{id}/comments/count는 인증 없이 접근 가능")
    void publicGetCommentsCount_WithoutAuth_Allowed() throws Exception {
        // Mock 데이터 설정
        given(commentService.countByIssue(1L)).willReturn(new CountCommentResponse(1L, 0L));

        mockMvc.perform(get("/issues/1/comments/count"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // 555 변경: GET /issues/{id}/navigation 공개 엔드포인트 테스트 추가 (Mock 데이터 설정)
    @Test
    @DisplayName("공개 URL - GET /issues/{id}/navigation은 인증 없이 접근 가능")
    void publicGetNavigation_WithoutAuth_Allowed() throws Exception {
        // Mock 데이터 설정
        given(issueService.getIssueNavigation(eq(1L), any())).willReturn(new IssueNavigationResponse(null, null));

        mockMvc.perform(get("/issues/1/navigation"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("보호된 URL(POST /issues)은 인증 없이 접근 차단")
    void protectedPostUrl_WithoutAuth_Blocked() throws Exception {
        // 테스트 대상을 GET이 아닌 POST로 변경하여 보호 여부를 확인합니다.
        // POST, PATCH, DELETE 등은 여전히 인증이 필요합니다.
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden()); // JWT 필터가 없으므로 403 Forbidden
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 보호된 URL(POST /issues) 접근 허용")
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void protectedUrl_WithValidToken_Allowed() throws Exception {
        // given
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // UserService mock 설정 추가 - IssueController가 내부적으로 호출함
        UserResponseDTO mockUser = new UserResponseDTO(1L, "Test User", USER_EMAIL);
        given(userService.findUserByEmail(USER_EMAIL)).willReturn(mockUser);

        IssueResponse dummyResponse = new IssueResponse(
                1L, "Test Issue", "Test Description", "OPEN", 1L,
                List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        given(issueService.createIssue(any(), any())).willReturn(dummyResponse);

        String validIssueJson = "{\"title\":\"Test Issue\",\"description\":\"Test Description\"}";

        // 테스트 대상을 GET이 아닌 POST로 변경하여 토큰 인증을 테스트합니다.
        mockMvc.perform(post("/issues")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssueJson))
                .andDo(print())
                .andExpect(status().isCreated()); // 컨트롤러 로직에 따라 201 Created 또는 다른 성공 코드를 기대
    }

    @Test
    @DisplayName("잘못된 JWT 토큰으로 보호된 URL 접근 차단")
    void protectedUrl_WithInvalidToken_Blocked() throws Exception {
        // given
        String invalidToken = "invalid.token";
        given(jwtUtil.getUserEmailFromToken(invalidToken)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(invalidToken, USER_EMAIL)).willReturn(false);

        // 테스트 대상을 GET이 아닌 POST로 변경
        mockMvc.perform(post("/issues")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // JwtAuthenticationFilter에서 401 Unauthorized 반환
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
        // 보호된 URL에 인증 없이 요청을 보내 세션(JSESSIONID)이 생성되지 않는지 확인
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden()) // 1. 인증 실패 확인
                .andExpect(cookie().doesNotExist("JSESSIONID")); // 2. JSESSIONID 쿠키가 없는지 확인

        // 동일한 요청을 한 번 더 보내도 세션이 유지되지 않음을 재차 확인
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

      @Test
    @DisplayName("HTTP 메서드별 접근 제어 - GET /issues는 인증 없이 허용")
    void httpMethod_GET_AllowedWithoutAuth() throws Exception {
        mockMvc.perform(get("/issues"))
                .andDo(print())
                .andExpect(status().isOk());  // 인증 없이도 200 OK
    }

    @Test
    @DisplayName("HTTP 메서드별 접근 제어 - POST")
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void httpMethod_POST_Allowed() throws Exception {
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // UserService mock 설정 추가 - IssueController가 내부적으로 호출함
        UserResponseDTO mockUser = new UserResponseDTO(1L, "Test User", USER_EMAIL);
        given(userService.findUserByEmail(USER_EMAIL)).willReturn(mockUser);

        IssueResponse dummyResponse = new IssueResponse(
                1L, "Test Issue", "Test Description", "OPEN", 1L,
                List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        given(issueService.createIssue(any(), any())).willReturn(dummyResponse);

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
        // given: 인증 관련 설정 (기존과 동일)
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // given: IssueService의 deleteIssue 메소드가 호출될 때 아무것도 하지 않도록 설정 (성공 시나리오)
        doNothing().when(issueService).deleteIssue(1L);

        // when & then
        mockMvc.perform(delete("/issues/1")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andDo(print())
                // DELETE는 일반적으로 204 No Content를 반환
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JWT 필터 체인 순서 확인")
    void filterChain_JwtBeforeUsernamePassword() throws Exception {
        // JWT 필터가 UsernamePasswordAuthenticationFilter보다 먼저 실행되는지 확인
        // 잘못된 토큰으로 401이 반환되면 JWT 필터가 먼저 실행된 것
        String invalidToken = "invalid.token";
        given(jwtUtil.getUserEmailFromToken(invalidToken))
                .willThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(post("/issues")
                        .header("Authorization", "Bearer " + invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("토큰 처리 중 오류가 발생했습니다.")));
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 /auth/logout 접근 허용")
    void logout_WithValidToken_Allowed() throws Exception {
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 /auth/users/{id} 접근 허용")
    void getUser_WithValidToken_Allowed() throws Exception {
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // 333변경: AuthService의 findUserById 메소드가 호출될 때 더미 응답 반환
        UserResponseDTO dummyUser = new UserResponseDTO(1L, "Test User", USER_EMAIL);
        given(userService.findUserById(1L)).willReturn(dummyUser);

        mockMvc.perform(get("/auth/users/1")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
/*

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @DisplayName("permitAll()된 GET /issues는 인증 없이 접근 가능")
    void publicGetIssues_WithoutAuth_Allowed() throws Exception {
        // GET /issues는 이제 permitAll이므로 403 Forbidden이 아닌 200 OK를 기대해야 합니다.
        mockMvc.perform(get("/issues"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("보호된 URL(POST /issues)은 인증 없이 접근 차단")
    void protectedPostUrl_WithoutAuth_Blocked() throws Exception {
        // 테스트 대상을 GET이 아닌 POST로 변경하여 보호 여부를 확인합니다.
        // POST, PATCH, DELETE 등은 여전히 인증이 필요합니다.
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden()); // JWT 필터가 없으므로 403 Forbidden
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 보호된 URL(POST /issues) 접근 허용")
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void protectedUrl_WithValidToken_Allowed() throws Exception {
        // given
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        IssueResponse dummyResponse = new IssueResponse(
                1L, "Test Issue", "Test Description", "OPEN", 1L,
                List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        given(issueService.createIssue(any(), any())).willReturn(dummyResponse);

        String validIssueJson = "{\"title\":\"Test Issue\",\"description\":\"Test Description\"}";

        // 테스트 대상을 GET이 아닌 POST로 변경하여 토큰 인증을 테스트합니다.
        mockMvc.perform(post("/issues")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssueJson))
                .andDo(print())
                .andExpect(status().isCreated()); // 컨트롤러 로직에 따라 201 Created 또는 다른 성공 코드를 기대
    }

    @Test
    @DisplayName("잘못된 JWT 토큰으로 보호된 URL 접근 차단")
    void protectedUrl_WithInvalidToken_Blocked() throws Exception {
        // given
        String invalidToken = "invalid.token";
        given(jwtUtil.getUserEmailFromToken(invalidToken)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(invalidToken, USER_EMAIL)).willReturn(false);

        // 테스트 대상을 GET이 아닌 POST로 변경
        mockMvc.perform(post("/issues")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // JwtAuthenticationFilter에서 401 Unauthorized 반환
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
        // 보호된 URL에 인증 없이 요청을 보내 세션(JSESSIONID)이 생성되지 않는지 확인
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden()) // 1. 인증 실패 확인
                .andExpect(cookie().doesNotExist("JSESSIONID")); // 2. JSESSIONID 쿠키가 없는지 확인

        // 동일한 요청을 한 번 더 보내도 세션이 유지되지 않음을 재차 확인
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
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
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void httpMethod_POST_Allowed() throws Exception {
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        IssueResponse dummyResponse = new IssueResponse(
                1L, "Test Issue", "Test Description", "OPEN", 1L,
                List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        given(issueService.createIssue(any(), any())).willReturn(dummyResponse);

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
        // given: 인증 관련 설정 (기존과 동일)
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // given: IssueService의 deleteIssue 메소드가 호출될 때 아무것도 하지 않도록 설정 (성공 시나리오)
        doNothing().when(issueService).deleteIssue(1L);

        // when & then
        mockMvc.perform(delete("/issues/1")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andDo(print())
                // then: 4xx 에러가 아닌, 성공 상태 코드인 200 OK를 기대하도록 변경
                .andExpect(status().isOk());
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


 */