package com.issueDive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.exception.AuthenticationFailedException;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.issueDive.util.JwtUtil;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.security.authentication.AuthenticationManager; // 9월 2일 변경: 추가
import com.issueDive.security.CustomUserDetailsService;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.MockitoAnnotations.openMocks;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 끄고 컨트롤러만 테스트
public class AuthControllerTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean  // 9월 2일 변경: AuthenticationManager mock 추가
    private AuthenticationManager authenticationManager;

    @MockitoBean  // 9월 2일 수정: CustomUserDetailsService Mock 추가 (빈 찾을 수 없음 에러 해결)
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /auth/signup → 201 Created")
    void signUp_success() throws Exception {
        // 요청 바디를 Map으로 만들어 DTO 생성자 유무와 무관하게 바인딩
        var req = Map.of(
                "username", "alice",
                "email", "alice@test.com",
                "password", "pw123"
        );
        var res = new UserResponseDTO(1L, "alice", "alice@test.com");
        given(userService.signUp(any())).willReturn(res);

        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andDo(print()) // 9월 2일 수정: 디버깅용 추가
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))  // 9월 2일 수정: ApiResponse 형식 체크
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /auth/login(username/password) → 200 OK")
    void login_success() throws Exception {
        var req = Map.of(
                "email", "alice@test.com",
                "password", "pw123"
        );
        String mockToken = "mock.jwt.token";
        var userResponse = new UserResponseDTO(1L, "alice", "alice@test.com");
        // findUserByEmail 사용 (login_withJWT_tokenGeneration 테스트와 동일하게)
        given(userService.findUserByEmail("alice@test.com")).willReturn(userResponse);
        given(jwtUtil.generateAccessToken(1L, "alice@test.com")).willReturn(mockToken);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(mockToken))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /auth/login - 인증 실패 → 401")
    void login_fail() throws Exception {
        var req = Map.of(
                "email", "nope@test.com",
                "password", "wrong"
        );
        given(userService.login(anyString(), anyString()))
                .willThrow(new AuthenticationFailedException());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/users/{id} → 200 OK")
    void getUserById_success() throws Exception {
        given(userService.findUserById(1L))
                .willReturn(new UserResponseDTO(1L, "alice", "alice@test.com"));

        mvc.perform(get("/auth/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    @DisplayName("GET /auth/users/{id} - 없음 → 404")
    void getUserById_notFound() throws Exception {
        given(userService.findUserById(999L))
                .willThrow(new UserNotFoundException(999L));

        mvc.perform(get("/auth/users/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /auth/users - 전체 목록 → 200 OK")
    void getAllUsers_success() throws Exception {
        var list = List.of(
                new UserResponseDTO(1L, "a", "a@test.com"),
                new UserResponseDTO(2L, "b", "b@test.com")
        );
        given(userService.getAllUsers()).willReturn(list);

        mvc.perform(get("/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("a@test.com"))
                .andExpect(jsonPath("$.data[1].username").value("b"));
    }

    @Test
    @DisplayName("DELETE /auth/user/{id} → 200 OK")
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mvc.perform(delete("/auth/user/{id}", 1L))
                .andExpect(status().isOk());
    }
    // ============== 9월 2일 추가: JWT 관련 테스트 ==============

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
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(14400));

        // JWT 토큰 생성 메서드가 호출되었는지 검증
        verify(jwtUtil, times(1)).generateAccessToken(1L, "alice@test.com");
    }

    @Test
    @DisplayName("POST /auth/logout - 로그아웃 응답 확인")
    void logout_success() throws Exception {
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("로그아웃되었습니다. 클라이언트에서 토큰을 삭제해주세요."))
                .andExpect(jsonPath("$.data.instruction").value("localStorage에서 accessToken을 제거하세요."));
    }

}
