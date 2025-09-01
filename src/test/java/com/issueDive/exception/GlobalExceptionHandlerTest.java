package com.issueDive.exception;

import com.issueDive.exception.NotFoundException;
import com.issueDive.service.IssueService;;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.issueDive.service.UserService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@SpringBootTest // 전체 스프링 컨텍스트 로딩
@AutoConfigureMockMvc(addFilters = false) // 필터(보안) 비활성화
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private UserService userService;

    @Test
    public void getIssue_notFound() throws Exception {
        // given : 서비스에서 예외 발생하도록 설정
        Mockito.when(issueService.getIssue(anyLong()))
                .thenThrow(new NotFoundException("Issue with id 999 not found"));

        // when : GET 요청 실행
        mockMvc.perform(get("/issues/999"))
                // then : 404 상태 및 에러 JSON 검증
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("IssueNotFound"))
                .andExpect(jsonPath("$.error.message")
                        .value("Issue with id 999 not found"));
    }

    // UserNotFound -> 404
    @Test
    public void getUserById_userNotFound() throws Exception {
        Mockito.when(userService.findUserById(999L))
                .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/auth/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UserNotFound"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    // DuplicateEmail -> 409
    @Test
    public void signUp_duplicateEmail_conflict() throws Exception {
        Mockito.when(userService.signUp(any()))
                .thenThrow(new DuplicateEmailException("dup@test.com"));

        String body = """
                {
                  "username": "bob",
                  "email": "dup@test.com",
                  "password": "pw"
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
        Mockito.when(userService.login(anyString(), anyString()))
                .thenThrow(new AuthenticationFailedException());

        String body = """
                {
                  "username": "alice@test.com",
                  "password": "wrong"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AuthenticationFailed"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }


}
