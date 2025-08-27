package com.example.issueDive.exception;

import com.example.issueDive.service.IssueService;
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

@SpringBootTest // 전체 스프링 컨텍스트 로딩
@AutoConfigureMockMvc(addFilters = false) // 필터(보안) 비활성화
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

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
}
