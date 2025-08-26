package com.example.issueDive.controller;

import com.example.issueDive.dto.CreateIssueRequest;
import com.example.issueDive.dto.IssueResponse;
import com.example.issueDive.service.IssueService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
// import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false) // 필터(보안) 비활성화
@WebMvcTest(IssueController.class) // IssueController만 로딩
public class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가상의 HTTP 요청, 응답을 시뮬레이트하는 객체

    @MockitoBean
    private IssueService issueService; // 서비스 레이어는 Mock으로 대체해 컨트롤러만 테스트

    @Test
    public void createIssue_success() throws Exception {

        // given
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "OPEN", 1L, 2L, null, null);

        Mockito.when(issueService.createIssue(any(CreateIssueRequest.class), anyLong())).thenReturn(mockResponse);

        String requestBody = """
            {
                "title": "제목",
                "description": "설명",
                "assignee_id": 2
            }
            """;

        // when: HTTP POST 요청 시뮬레이션
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)) // 요청 본문에 JSON 설정
                // then: 응답 확인
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(jsonPath("$.success").value(true)) // 응답 JSON 성공 여부 확인
                .andExpect(jsonPath("$.data.id").value(1)) // 데이터 내 id 값 검증
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    public void getIssue_success() throws Exception {
        // given
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "OPEN", 1L, 2L, null, null);
        Mockito.when(issueService.getIssue(1L)).thenReturn(mockResponse); // issueService.getIssue(1L) 호출 시 mockResponse 반환하도록 Stub 설정

        // when: HTTP GET 요청 시뮬레이션
        mockMvc.perform(get("/issues/1"))
                // then: 응답 확인
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("제목"));
    }
}
