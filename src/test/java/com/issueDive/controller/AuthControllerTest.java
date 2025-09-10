package com.issueDive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.exception.AuthenticationFailedException;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.service.UserService;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.issueDive.dto.RefreshTokenRequest;
import com.issueDive.service.RedisService;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.MockitoAnnotations.openMocks;
/**
 * @WebMvcTest: 웹 계층(컨트롤러)에 대한 슬라이스 테스트를 진행합니다.
 * @AutoConfigureMockMvc: MockMvc를 자동으로 설정하며, addFilters = false를 통해
 * AuthController의 공개 API 테스트 시 Spring Security 필터를 적용하지 않습니다.
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mvc; // HTTP 요청을 시뮬레이션하는 Mock 객체

    @Autowired
    private ObjectMapper om; // Java 객체와 JSON 간의 변환을 담당

    // --- MockitoBean: 테스트 대상 컨트롤러의 의존성을 가짜(Mock) 객체로 주입 ---
    @MockitoBean
    private UserService userService;

    // SecurityConfig를 로드하지 않으므로, AuthController가 의존하는 Bean들을 Mock으로 등록
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    // Security Filter Chain이 로드될 때를 대비하여 의존성 Mock Bean 추가
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // Redis 서비스 Mock 추가
    @MockitoBean
    private RedisService redisService;

    @Test
    @DisplayName("[SUCCESS] POST /auth/signup - 회원가입 성공")
    void signUp_success() throws Exception {
        // given: 회원가입 요청 데이터와 예상되는 응답 DTO 설정
        var requestBody = Map.of(
                "username", "alice",
                "email", "alice@test.com",
                "password", "password123"
        );
        var responseDto = new UserResponseDTO(1L, "alice", "alice@test.com");

        // userService.signUp 메서드가 호출될 때 위에서 정의한 DTO를 반환하도록 설정
        given(userService.signUp(any())).willReturn(responseDto);

        // when & then: API를 호출하고 응답을 검증
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(requestBody)))
                .andExpect(status().isCreated()) // 201 Created 상태 코드 확인
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("[SUCCESS] POST /auth/login - 로그인 성공")
    void login_success() throws Exception {
        // given: 로그인 요청 데이터 설정
        var requestBody = Map.of(
                "email", "alice@test.com",  //  ** LoginRequestDTO의 필드명인 'email'로 수정
                "password", "password123"
        );
        var userResponse = new UserResponseDTO(1L, "alice", "alice@test.com");
        var mockToken = "mock-access-token";
        var mockRefreshToken = "mock-refresh-token";

        // 컨트롤러의 로그인 로직에 필요한 Mocking 설정
        given(userService.findUserByEmail(anyString())).willReturn(userResponse);
        given(jwtUtil.generateAccessToken(anyLong(), anyString())).willReturn(mockToken);
        given(jwtUtil.generateRefreshToken(anyLong(), anyString())).willReturn(mockRefreshToken); // 9월10일 수정
        given(jwtUtil.getRefreshExpiration()).willReturn(604800L); // 9월10일 수정
        doNothing().when(redisService).saveRefreshToken(anyString(), anyString(), anyLong()); // 9월10일 수정


        // when & then
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(requestBody)))
                .andExpect(status().isOk()) // 200 OK 상태 코드 확인
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value(mockToken))
                .andExpect(jsonPath("$.data.refreshToken").value(mockRefreshToken))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("alice@test.com"));

        verify(redisService, times(1)).saveRefreshToken(eq("alice@test.com"), eq(mockRefreshToken), anyLong());
    }

    @Test
    @DisplayName("[FAIL] POST /auth/login - 로그인 실패 (인증 오류)")
    void login_fail() throws Exception {
        var requestBody = Map.of(
                "email", "nope@test.com",
                "password", "wrong"
        );

        // 인증 실패 시 AuthenticationFailedException이 발생하도록 설정
        given(authenticationManager.authenticate(any())).willThrow(new AuthenticationFailedException());

        // when & then
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized 상태 코드 확인
    }

    // 로그아웃 테스트 추가
    @Test
    @DisplayName("[SUCCESS] POST /auth/logout - 로그아웃 성공 (토큰 블랙리스트 추가)")
    @WithMockUser(username = "alice@test.com")
    void logout_success() throws Exception {
        String accessToken = "valid-access-token";
        String bearerToken = "Bearer " + accessToken;

        given(jwtUtil.getRemainingExpirationTime(accessToken)).willReturn(3600L); // 1시간 남음
        doNothing().when(redisService).addToBlacklist(anyString(), anyLong());
        doNothing().when(redisService).deleteRefreshToken(anyString());

        mvc.perform(post("/auth/logout")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("로그아웃되었습니다."))
                .andExpect(jsonPath("$.data.instruction").value("클라이언트에서 토큰을 삭제해주세요."));

        // 9월10일 수정 - 블랙리스트와 리프레시 토큰 삭제 검증
        verify(redisService, times(1)).addToBlacklist(eq(accessToken), eq(3600L));
        verify(redisService, times(1)).deleteRefreshToken("alice@test.com");
    }

    @Test
    @DisplayName("[SUCCESS] GET /auth/users/{id} - 특정 사용자 조회 성공")
    void getUserById_success() throws Exception {
        // given
        given(userService.findUserById(1L))
                .willReturn(new UserResponseDTO(1L, "alice", "alice@test.com"));

        // when & then
        mvc.perform(get("/auth/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    @DisplayName("[FAIL] GET /auth/users/{id} - 존재하지 않는 사용자 조회")
    void getUserById_notFound() throws Exception {
        // given
        given(userService.findUserById(999L))
                .willThrow(new UserNotFoundException(999L));

        // when & then
        mvc.perform(get("/auth/users/{id}", 999L))
                .andExpect(status().isNotFound()); // 404 Not Found 상태 코드 확인
    }

    @Test
    @DisplayName("[SUCCESS] GET /auth/users - 전체 사용자 목록 조회 성공")
    void getAllUsers_success() throws Exception {
        // given
        var userList = List.of(
                new UserResponseDTO(1L, "a", "a@test.com"),
                new UserResponseDTO(2L, "b", "b@test.com")
        );
        given(userService.getAllUsers()).willReturn(userList);

        // when & then
        mvc.perform(get("/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].email").value("a@test.com"))
                .andExpect(jsonPath("$.data[1].username").value("b"));
    }

    @Test
    @DisplayName("[SUCCESS] DELETE /auth/user/{id} - 사용자 삭제 성공")
    void deleteUser_success() throws Exception {
        // given
        doNothing().when(userService).deleteUser(1L);

        // when & then
        mvc.perform(delete("/auth/user/{id}", 1L))
                .andExpect(status().isOk());
    }

    // ==============JWT 관련 테스트 ==============

    // 9월10일 수정 - JWT 토큰 생성 테스트 수정 (리프레시 토큰 포함)
    @Test
    @DisplayName("POST /auth/login - JWT 토큰 생성 확인 (리프레시 토큰 포함)")
    void login_withJWT_tokenGeneration() throws Exception {
        var req = Map.of(
                "email", "alice@test.com",
                "password", "password123"
        );

        String mockAccessToken = "mock.jwt.access.token";
        String mockRefreshToken = "mock.jwt.refresh.token"; // 9월10일 수정
        var userResponse = new UserResponseDTO(1L, "alice", "alice@test.com");

        given(userService.findUserByEmail("alice@test.com")).willReturn(userResponse);
        given(jwtUtil.generateAccessToken(1L, "alice@test.com")).willReturn(mockAccessToken);
        given(jwtUtil.generateRefreshToken(1L, "alice@test.com")).willReturn(mockRefreshToken); // 9월10일 수정
        given(jwtUtil.getRefreshExpiration()).willReturn(604800L); // 9월10일 수정

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(mockAccessToken))
                .andExpect(jsonPath("$.data.refreshToken").value(mockRefreshToken)) // 9월10일 수정
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(14400));

        // 토큰 생성 메서드 호출 검증
        verify(jwtUtil, times(1)).generateAccessToken(1L, "alice@test.com");
        verify(jwtUtil, times(1)).generateRefreshToken(1L, "alice@test.com");
    }

    // 9월10일 수정 - 토큰 갱신 테스트 추가
    @Test
    @DisplayName("[SUCCESS] POST /auth/refresh - 토큰 갱신 성공")
    void refreshToken_success() throws Exception {
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        String userEmail = "alice@test.com";
        var userResponse = new UserResponseDTO(1L, "alice", userEmail);

        var requestBody = Map.of("refreshToken", refreshToken);

        given(jwtUtil.getUserEmailFromToken(refreshToken)).willReturn(userEmail);
        given(jwtUtil.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtUtil.validateToken(refreshToken, userEmail)).willReturn(true);
        given(redisService.validateRefreshToken(userEmail, refreshToken)).willReturn(true);
        given(userService.findUserByEmail(userEmail)).willReturn(userResponse);
        given(jwtUtil.generateAccessToken(1L, userEmail)).willReturn(newAccessToken);

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.data.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.data.user.email").value(userEmail));
    }

    // 9월10일 수정 - 유효하지 않은 리프레시 토큰 테스트 추가
    @Test
    @DisplayName("[FAIL] POST /auth/refresh - 유효하지 않은 리프레시 토큰")
    void refreshToken_invalidToken() throws Exception {
        String invalidToken = "invalid-refresh-token";
        var requestBody = Map.of("refreshToken", invalidToken);

        given(jwtUtil.getUserEmailFromToken(invalidToken)).willReturn("test@example.com");
        given(jwtUtil.isRefreshToken(invalidToken)).willReturn(false);

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }
}
