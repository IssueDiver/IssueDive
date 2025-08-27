package com.example.issueDive.controller;

import com.example.issueDive.dto.CreateIssueRequest;
import com.example.issueDive.dto.IssueFilterRequest;
import com.example.issueDive.dto.IssueResponse;
import com.example.issueDive.exception.ErrorCode;
import com.example.issueDive.exception.NotFoundException;
import com.example.issueDive.exception.ValidationException;
import com.example.issueDive.service.IssueService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
// import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "OPEN", 1L, 2L,
                List.of(1L, 2L),  // 라벨 ID 리스트 샘플, 테스트에 맞춰 변경 가능
                LocalDateTime.now(),  // createdAt 예시
                LocalDateTime.now()   // updatedAt 예시
        );

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

    /**
     * 다중 조회
     */
    @Test
    void getIssues_returnsPagedResults() throws Exception {
        // given: Mock Service가 반환할 Page 객체를 미리 생성
        List<IssueResponse> issueList = List.of(
                new IssueResponse(1L, "첫 번째 이슈", "설명 1", "OPEN", 1L, 2L, List.of(), LocalDateTime.now(), LocalDateTime.now()),
                new IssueResponse(2L, "두 번째 이슈", "설명 2", "OPEN", 1L, 3L, List.of(), LocalDateTime.now(), LocalDateTime.now())
        );
        Page<IssueResponse> mockPage = new PageImpl<>(issueList, PageRequest.of(0, 10), issueList.size());

        // issueService.getFilteredIssues()가 어떤 IssueFilterRequest 객체로 호출되든, 위에서 만든 mockPage 객체를 반환하도록 설정
        Mockito.when(issueService.getFilteredIssues(any(IssueFilterRequest.class)))
                .thenReturn(mockPage);

        // when-then: API를 호출하고 응답을 검증
        mockMvc.perform(get("/issues")
                        .param("status", "OPEN")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(0));
    }

    /**
     * 단건 조회
     */
    @Test
    public void getIssue_success() throws Exception {
        // given
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "OPEN", 1L, 2L,
                List.of(1L, 2L),  // 라벨 ID 리스트 샘플, 테스트에 맞춰 변경 가능
                LocalDateTime.now(),  // createdAt 예시
                LocalDateTime.now()   // updatedAt 예시
        );
        Mockito.when(issueService.getIssue(1L)).thenReturn(mockResponse); // issueService.getIssue(1L) 호출 시 mockResponse 반환하도록 Stub 설정

        // when: HTTP GET 요청 시뮬레이션
        mockMvc.perform(get("/issues/1"))
                // then: 응답 확인
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    /**
     * 이슈 상태 변경 API 테스트
     * 성공
     */
    @Test
    void changeIssueStatus_success() throws Exception {

        // given
        IssueResponse mockResponse = new IssueResponse(1L, "제목", "설명", "CLOSED", 1L, 2L,
                List.of(1L, 2L),  // 라벨 ID 리스트 샘플, 테스트에 맞춰 변경 가능
                LocalDateTime.now(),  // createdAt 예시
                LocalDateTime.now()   // updatedAt 예시
        );
        Mockito.when(issueService.changeIssueStatus(1L, "CLOSED")).thenReturn(mockResponse);

        String requestBody = """
        {
            "status": "CLOSED"
        }
        """;

        // when: HTTP PATCH 요청
        mockMvc.perform(patch("/issues/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // then: 성공 응답 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    /**
     * 이슈 상태 변경 API 테스트
     * 실패: Invalid Status
     */
    @Test
    void changeIssueStatus_invalidStatus_badRequest() throws Exception {

        // given
        Mockito.when(issueService.changeIssueStatus(Mockito.eq(1L), Mockito.eq("INVALID")))
                .thenThrow(new ValidationException(ErrorCode.InvalidStatus, "status must be either OPEN or CLOSED"));

        String requestBody = """
        {
            "status": "INVALID"
        }
        """;

        // when: HTTP PATCH 요청
        mockMvc.perform(patch("/issues/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // then: BadRequest (400) Invalid Status 에러 응답 확인
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("InvalidStatus"));
    }

    /**
     * 이슈 상태 변경 API 테스트
     * 실패: Issue Not Found
     */
    @Test
    void changeIssueStatus_issueNotFound_notFound() throws Exception {

        // given
        Mockito.when(issueService.changeIssueStatus(Mockito.eq(99L), Mockito.anyString()))
                .thenThrow(new NotFoundException("Issue not found"));

        String requestBody = """
        {
            "status": "CLOSED"
        }
        """;

        // when: HTTP PATCH 요청
        mockMvc.perform(patch("/issues/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // then: NotFound (404) 에러 응답 확인
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("IssueNotFound"));
    }


}
