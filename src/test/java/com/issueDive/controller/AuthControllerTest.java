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
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.issueDive.service.TokenBlackListService;
import com.issueDive.config.SecurityConfig;
import com.issueDive.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.ComponentScan;
import static org.mockito.MockitoAnnotations.openMocks;
/**
 * @WebMvcTest: 웹 계층(컨트롤러)에 대한 슬라이스 테스트를 진행합니다.
 * @AutoConfigureMockMvc: MockMvc를 자동으로 설정하며, addFilters = false를 통해
 * AuthController의 공개 API 테스트 시 Spring Security 필터를 적용하지 않습니다.
*/
@WebMvcTest(
        controllers = AuthController.class,
        // 9월9일 수정: WebMvc 슬라이스에서 SecurityConfig/JwtAuthenticationFilter 제외 (중복 빈 충돌, 필터 로딩 방지)
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
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

    // AuthController 생성자 주입 대상 추가 (누락 시 컨텍스트 실패)
    @MockitoBean private TokenBlackListService tokenBlackListService;

    // Security 필터 비활성화했어도, 로딩 중 참조될 수 있어 방어적으로 유지
    @MockitoBean private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("[SUCCESS] POST /auth/signup - 회원가입 성공")
    void signUp_success() throws Exception {
        // given: 회원가입 요청 데이터와 예상되는 응답 DTO 설정
        var requestBody = Map.of(
                "username", "alice",
                "email", "alice@test.com",
                "password", "pw123"
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
                "password", "pw123"
        );
        var userResponse = new UserResponseDTO(1L, "alice", "alice@test.com");
        var mockToken = "mock-access-token";

        // 컨트롤러의 로그인 로직에 필요한 Mocking 설정
        given(userService.findUserByEmail(anyString())).willReturn(userResponse);
        given(jwtUtil.generateAccessToken(anyLong(), anyString())).willReturn(mockToken);

        // when & then
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(requestBody)))
                .andExpect(status().isOk()) // 200 OK 상태 코드 확인
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value(mockToken))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(14400))
                .andExpect(jsonPath("$.data.user.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("[FAIL] POST /auth/login - 로그인 실패 (인증 오류)")
    void login_fail() throws Exception {
        var requestBody = Map.of(
                "email", "nope@test.com",
                "password", "wrong"
        );

        // 인증 실패 시 AuthenticationFailedException이 발생하도록 설정
        willThrow(new RuntimeException("no user"))
                .given(userService).findUserByEmail(anyString());

        // when & then
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized 상태 코드 확인
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
        willThrow(new com.issueDive.exception.UserNotFoundException(999L))
                .given(userService).findUserById(999L);

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

    @Test
    @DisplayName("POST /auth/login - JWT 토큰 생성 확인")
    void login_withJWT_tokenGeneration() throws Exception {
        var req = Map.of(
                "email", "alice@test.com",
                "password", "pw123"
        );

        String mockToken = "mock.jwt.token";
        var userResponse = new UserResponseDTO(1L, "alice", "alice@test.com");

        given(userService.findUserByEmail("alice@test.com")).willReturn(userResponse);
        given(jwtUtil.generateAccessToken(1L, "alice@test.com")).willReturn(mockToken);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(mockToken))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
               // .andExpect(jsonPath("$.data.expiresIn").value(14400));

        // JWT 토큰 생성 메서드가 호출되었는지 검증
        verify(jwtUtil, times(1)).generateAccessToken(1L, "alice@test.com");
    }

    @Test
    @DisplayName("[SUCCESS] POST /auth/logout - Authorization 헤더 포함시 200 OK")
    void logout_success() throws Exception {
        String rawToken = "eyJhbGciOiJIUzI1NiJ9.mock";
        String bearerToken = "Bearer " + rawToken;

        // 토큰 만료 시간을 미래로 설정 → 블랙리스트에 추가되도록 함
        var future = new java.util.Date(System.currentTimeMillis() + 3600_000L); // +1시간
        given(jwtUtil.getExpirationDateFromToken(rawToken)).willReturn(future);

        mvc.perform(post("/auth/logout").header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("로그아웃되었습니다."))
                .andExpect(jsonPath("$.data.instruction").value("서버에서 토큰이 무효화되었습니다."));

        // 블랙리스트에 정상적으로 호출되었는지 검증
        verify(tokenBlackListService, times(1)).addToBlackList(eq(rawToken), anyLong());
    }

}

