package com.issueDive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.dto.LoginRequestDTO;
import com.issueDive.dto.UserRequestDTO;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.exception.AuthenticationFailedException;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 끄고 컨트롤러만 테스트
public class AuthControllerTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    UserService userService;

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /auth/login(username/password) → 200 OK")
    void login_success() throws Exception {
        // LoginRequestDTO는 username/password로 변경됨
        var req = Map.of(
                "username", "alice@test.com",
                "password", "pw123"
        );
        var res = new UserResponseDTO(1L, "alice", "alice@test.com");
        // 컨트롤러가 userService.login(username, password) 호출한다고 가정
        given(userService.login(anyString(), anyString())).willReturn(res);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /auth/login - 인증 실패 → 401")
    void login_fail() throws Exception {
        var req = Map.of(
                "username", "nope@test.com",
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
}
