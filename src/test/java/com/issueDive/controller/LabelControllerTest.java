package com.issueDive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.IssueLabelsResponse;
import com.issueDive.dto.LabelResponse;
import com.issueDive.exception.*;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.service.IssueLabelService;
import com.issueDive.service.LabelService;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * @WebMvcTest: LabelController와 관련된 웹 계층 빈들만 로드합니다.
 * @AutoConfigureMockMvc: MockMvc를 자동으로 설정하며, Spring Security 필터를 활성화합니다.
 * @WithMockUser: 이 클래스의 모든 테스트에 로그인한 상태를 전역 적용합니다.
 */
@WithMockUser
@AutoConfigureMockMvc
@WebMvcTest(LabelController.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- MockitoBean: 테스트 대상 컨트롤러의 의존성을 가짜(Mock) 객체로 주입 ---
    @MockitoBean
    private LabelService labelService;

    @MockitoBean
    private IssueLabelService issueLabelService;

    // Security Filter Chain 구성을 위해 필요한 의존성 Mock Bean 추가
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final String API_PREFIX = "/api";

    @Test
    @DisplayName("[SUCCESS] POST /labels - 라벨 생성 성공")
    void createLabel_success() throws Exception {
        // given: 서비스가 반환할 Mock 응답 데이터 생성
        LabelResponse mockResponse = LabelResponse.builder().id(10L).name("bug").color("#FF0000")
                .issueOpenCount(0L)  //새 라벨은 openCount = 0
                .build();
        Mockito.when(labelService.createLabel(any(CreateLabelRequest.class))).thenReturn(mockResponse);

        String requestBody = """
        {
          "name": "bug",
          "color": "#FF0000",
          "description": "버그 관련 이슈"
        }
        """;

        // when & then: API 호출 및 응답 검증
        mockMvc.perform(post(API_PREFIX + "/labels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("bug"))
                .andExpect(jsonPath("$.data.issueOpenCount").value(0));  //검증 추가
    }

    @Test
    @DisplayName("[FAIL] POST /labels - 라벨 이름 중복으로 생성 실패")
    void createLabel_duplicateName_BadRequest() throws Exception {
        // given
        String requestBody = """
        { "name": "bug", "color": "#FF0000" }
        """;
        // 서비스가 중복 예외를 던지도록 설정
        Mockito.when(labelService.createLabel(any(CreateLabelRequest.class)))
                .thenThrow(new ValidationException(ErrorCode.DuplicateLabel, "이미 존재하는 라벨 이름입니다."));

        // when & then
        mockMvc.perform(post(API_PREFIX + "/labels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DuplicateLabel"));
    }

    @Test
    @DisplayName("[SUCCESS] GET /labels - 전체 라벨 목록 조회 성공")
    void getLabels_success() throws Exception {
        // given
        List<LabelResponse> mockList = List.of(
                LabelResponse.builder().id(1L).name("bug").issueOpenCount(2L).build(),
                LabelResponse.builder().id(2L).name("feature").issueOpenCount(0L).build()
        );
        Mockito.when(labelService.getLabels()).thenReturn(mockList);

        // when & then
        mockMvc.perform(get(API_PREFIX + "/labels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("bug"))
                .andExpect(jsonPath("$.data[0].issueOpenCount").value(2))    //count 검증 추가
                .andExpect(jsonPath("$.data[1].name").value("feature"))
                .andExpect(jsonPath("$.data[1].issueOpenCount").value(0));   //count 검증 추가
    }

    @Test
    @DisplayName("[SUCCESS] GET /labels/{labelId} - 특정 라벨 조회 성공")
    void getLabel_success() throws Exception {
        // given
        LabelResponse mockResponse = LabelResponse.builder().id(10L).name("bug").color("#FF0000")
                .issueOpenCount(5L)  //OPEN이슈 5개
                .build();
        Mockito.when(labelService.getLabel(10L)).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get(API_PREFIX + "/labels/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("bug"))
                .andExpect(jsonPath("$.data.issueOpenCount").value(5));  //count 검증 추가
    }

    @Test
    @DisplayName("[FAIL] GET /labels/{labelId} - 존재하지 않는 라벨 조회")
    void getLabel_labelNotFound_notFound() throws Exception {
        // given
        Mockito.when(labelService.getLabel(99L)).thenThrow(new LabelNotFoundException("라벨을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get(API_PREFIX + "/labels/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LabelNotFound"));
    }

    @Test
    @DisplayName("[SUCCESS] PATCH /labels/{labelId} - 라벨 수정 성공")
    void updateLabel_success() throws Exception {
        // given
        Long labelId = 10L;
        String requestBody = """
            { "name": "critical bug", "color": "#000000" }
            """;
        LabelResponse mockResponse = LabelResponse.builder().id(labelId).name("critical bug").color("#000000").build();
        Mockito.when(labelService.updateLabel(eq(labelId), any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(patch(API_PREFIX + "/labels/{labelId}", labelId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("critical bug"))
                .andExpect(jsonPath("$.data.color").value("#000000"));
    }

    @Test
    @DisplayName("[SUCCESS] DELETE /labels/{labelId} - 이슈-라벨이 매핑된 라벨 삭제 성공")
    void deleteLabel_success() throws Exception {
        // given
        Long labelId = 10L;
        Mockito.doNothing().when(labelService).deleteLabel(labelId);

        // when & then
        mockMvc.perform(delete(API_PREFIX + "/labels/{id}", labelId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Label 10 deleted successfully"));
    }

    @Test
    @DisplayName("[SUCCESS] DELETE /labels/{labelId} - 이슈-라벨 매핑 없는 라벨 삭제 성공")
    void deleteLabel_noMapping_success() throws Exception {
        Long labelId = 20L;
        Mockito.doNothing().when(labelService).deleteLabel(labelId);

        mockMvc.perform(delete(API_PREFIX + "/labels/{id}", labelId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Label 20 deleted successfully"));
    }

    @Test
    @DisplayName("[SUCCESS] POST /issues/{issueId}/labels - 이슈에 라벨 추가 성공")
    void addLabelsToIssue_success() throws Exception {
        // given
        Long issueId = 1L;
        List<Long> labelIds = List.of(10L, 20L);
        List<IssueLabelsResponse.LabelSummary> mockLabels = List.of(
                new IssueLabelsResponse.LabelSummary(10L, "bug", "#d73a4a"),
                new IssueLabelsResponse.LabelSummary(20L, "feature", "#007bff")
        );
        IssueLabelsResponse mockResponse = IssueLabelsResponse.builder().id(issueId).labels(mockLabels).build();
        Mockito.when(issueLabelService.addLabelsToIssue(issueId, labelIds)).thenReturn(mockResponse);

        String requestBody = "[10, 20]";

        // when & then
        mockMvc.perform(post(API_PREFIX + "/issues/{issueId}/labels", issueId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(issueId))
                .andExpect(jsonPath("$.data.labels[0].id").value(10))
                .andExpect(jsonPath("$.data.labels[1].id").value(20));

        // issueLabelService의 메서드가 정확한 인자들로 호출되었는지 검증
        Mockito.verify(issueLabelService).addLabelsToIssue(issueId, labelIds);
    }

    @Test
    @DisplayName("[SUCCESS] DELETE /issues/{issueId}/labels/{labelId} - 이슈에서 라벨 제거 성공")
    void deleteLabelFromIssue_success() throws Exception {
        // given
        Long issueId = 1L;
        Long labelId = 20L;
        IssueLabelsResponse mockResponse = IssueLabelsResponse.builder().id(issueId)
                .labels(List.of(new IssueLabelsResponse.LabelSummary(labelId, "feature", "#007bff")))
                .build();
        Mockito.when(issueLabelService.deleteLabelFromIssue(issueId, labelId)).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(delete(API_PREFIX + "/issues/{issueId}/labels/{labelId}", issueId, labelId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(issueId))
                .andExpect(jsonPath("$.data.labels[0].id").value(labelId))
                .andExpect(jsonPath("$.data.labels[0].name").value("feature"))
                .andExpect(jsonPath("$.data.labels[0].color").value("#007bff"));

        Mockito.verify(issueLabelService).deleteLabelFromIssue(issueId, labelId);
    }
}