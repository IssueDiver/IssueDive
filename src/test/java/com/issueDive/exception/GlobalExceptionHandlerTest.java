package com.issueDive.exception;

import com.issueDive.controller.AuthController;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.issueDive.service.UserService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;

/**
 * Controller에서 특정 예외가 발생했을 때, GlobalExceptionHandler가 그것을 잘 가로채서 우리가 원하는 JSON 에러 응답(예: 409 Conflict)을 만들어주는가?"를 검증하는 것이 목표이므로 @WebMvcTest 어노테이션을 사용하여 웹 계층만 로드함.
 * 다른 @Configuration의 영향을 받지 않고 순수하게 예외 처리 로직만 테스트하도록 함.
 * AuthController에 한해서 예외처리 테스트만 진행함 (다른 컨트롤러는 제외)
 *
 */
@WebMvcTest(controllers = AuthController.class) // 예외를 발생시킬 컨트롤러를 지정
@AutoConfigureMockMvc(addFilters = false)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // DuplicateEmail -> 409
    @Test
    public void signUp_duplicateEmail_conflict() throws Exception {
        Mockito.when(userService.signUp(any()))
                .thenThrow(new DuplicateEmailException("dup@test.com"));

        String body = """
                {
                  "username": "bob",
                  "email": "dup@test.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DuplicateEmail"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    // AuthenticationFailed -> 401
    @Test
    public void login_authenticationFailed_unauthorized() throws Exception {
        // given: userService.findUserByEmail이 호출되면 AuthenticationFailedException을 던지도록 설정
        Mockito.when(userService.findUserByEmail(anyString())).thenThrow(new AuthenticationFailedException());

        String body = """
                {
                  "email": "alice@test.com",
                  "password": "passwordWrong"
                }
                """;

        // when & then: API 호출 시 401 Unauthorized 응답을 기대
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AuthenticationFailed"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }
}