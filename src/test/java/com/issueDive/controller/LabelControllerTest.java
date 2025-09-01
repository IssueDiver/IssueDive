package com.issueDive.controller;

import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.IssueLabelsResponse;
import com.issueDive.dto.LabelResponse;
import com.issueDive.exception.*;
import com.issueDive.service.IssueLabelService;
import com.issueDive.service.IssueService;
import com.issueDive.service.LabelService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(LabelController.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelService labelService; // 서비스는 Mock으로 대체

    @MockitoBean
    private IssueLabelService issueLabelService;

    @MockitoBean
    private IssueService issueService;

    /**
     * 라벨 생성 테스트
     * 성공
     */
    @Test
    void createLabel_success() throws Exception {
        // given
        LabelResponse mock = LabelResponse.builder()
                .id(10L)
                .name("bug")
                .color("#FF0000")
                .description("버그 관련 이슈")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(labelService.createLabel(any(CreateLabelRequest.class)))
                .thenReturn(mock);

        String body = """
        {
          "name": "bug",
          "color": "#FF0000",
          "description": "버그 관련 이슈"
        }
        """;

        // when & then
        mockMvc.perform(post("/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())              // 201
                .andExpect(jsonPath("$.success").value(true)) // ApiResponse.success
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("bug"))
                .andExpect(jsonPath("$.data.color").value("#FF0000"));
    }

    /**
     * 라벨 생성 테스트
     * 실패: name 중복 시
     */
    @Test
    void createLabel_duplicateName_BadRequest() throws Exception {
        // given
        String body = """
                {
                  "name": "bug",
                  "color": "#FF0000",
                  "description": "버그 관련 이슈"
                }
                """;

        // 서비스가 중복 예외를 던지도록 스텁
        Mockito.when(labelService.createLabel(any(CreateLabelRequest.class)))
                .thenThrow(new ValidationException(ErrorCode.DuplicateLabel, "이미 존재하는 라벨 이름입니다."));

        // when & then
        mockMvc.perform(post("/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DuplicateLabel"))
                .andExpect(jsonPath("$.error.message").value("이미 존재하는 라벨 이름입니다."));
    }

    /**
     * 라벨 목록 조회
     * 성공
     */
    @Test
    void getLabels_success() throws Exception {
        // given
        LabelResponse label1 = LabelResponse.builder()
                .id(1L)
                .name("bug")
                .color("#FF0000")
                .description("버그 관련")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        LabelResponse label2 = LabelResponse.builder()
                .id(2L)
                .name("feature")
                .color("#00FF00")
                .description("기능 추가")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<LabelResponse> mockList = List.of(label1, label2);

        Mockito.when(labelService.getLabels()).thenReturn(mockList);

        // when & then
        mockMvc.perform(get("/labels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("bug"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("feature"));
    }

    /**
     * 라벨 목록 조회
     * 성공 - 데이터 없을 시
     */
    @Test
    void getLabels_emptyList() throws Exception {
        // given
        Mockito.when(labelService.getLabels()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/labels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 라벨 단일 조회
     * 성공
     */
    @Test
    void getLabel_success() throws Exception {
        // given
        LabelResponse mock = LabelResponse.builder()
                .id(10L)
                .name("bug")
                .color("#FF0000")
                .description("버그 관련 이슈")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(labelService.getLabel(10L)).thenReturn(mock);

        // when & then
        mockMvc.perform(get("/labels/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("bug"))
                .andExpect(jsonPath("$.data.color").value("#FF0000"));
    }


    /**
     * 라벨 단일 조회
     * 실패: 존재하지 않는 라벨 조회 시
     */
    @Test
    void getLabel_labelNotFound_notFound() throws Exception {
        // given
        Mockito.when(labelService.getLabel(99L))
                .thenThrow(new LabelNotFoundException("라벨을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/labels/99"))
                .andExpect(status().isNotFound())                 // 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LabelNotFound"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    /**
     * 라벨 수정
     * 성공
     */
    @Test
    void updateLabel_duplicateName_returnsBadRequest() throws Exception {
        // given
        Long labelId = 10L;
        String body = """
        {
          "name": "bug",
          "color": "#000000",
          "description": "중복 이름"
        }
        """;

        Mockito.when(labelService.updateLabel(eq(labelId), any()))
                .thenThrow(new ValidationException(ErrorCode.DuplicateLabel, "이미 존재하는 라벨 이름입니다."));

        // when & then
        mockMvc.perform(patch("/labels/{labelId}", labelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())                    // 400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DuplicateLabel"))
                .andExpect(jsonPath("$.error.message").value("이미 존재하는 라벨 이름입니다."));
    }

    /**
     * 라벨 수정
     * 실패: name 중복 시
     */
    @Test
    void updateLabel_duplicateName() throws Exception {
        // given
        Long labelId = 10L;
        String body = """
        {
          "name": "bug",
          "color": "#000000",
          "description": "중복 이름"
        }
        """;

        Mockito.when(labelService.updateLabel(eq(labelId), any()))
                .thenThrow(new ValidationException(ErrorCode.DuplicateLabel, "이미 존재하는 라벨 이름입니다."));

        // when & then
        mockMvc.perform(patch("/labels/{id}", labelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())                    // 400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DuplicateLabel"))      // <-- 여기
                .andExpect(jsonPath("$.error.message").value("이미 존재하는 라벨 이름입니다."));  // <-- 여기
    }

    /**
     * 라벨 수정
     * 실패: 존재하지 않는 라벨 수정 시
     */
    @Test
    void updateLabel_notFound() throws Exception {
        // given
        Long labelId = 999L;
        String body = """
        {
          "name": "nonexistent",
        "color": "#123456",
        "description": "없는 라벨"
        }
        """;

        Mockito.when(labelService.updateLabel(eq(labelId), any()))
                .thenThrow(new LabelNotFoundException("라벨을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(patch("/labels/{id}", labelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())               // 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LabelNotFound"))
                .andExpect(jsonPath("$.error.message").value("라벨을 찾을 수 없습니다."));
    }

    /**
     * 라벨 삭제
     * 성공
     */
    @Test
    void deleteLabel_success() throws Exception {
        // given
        Long labelId = 10L;

        Mockito.doNothing().when(labelService).deleteLabel(labelId);

        // when & then
        mockMvc.perform(delete("/labels/{id}", labelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Label 10 deleted successfully"));
    }

    /**
     * 라벨 삭제
     * 실패: 존재하지 않는 라벨 삭제 시
     */
    @Test
    void deleteLabel_labelNotFound_notFound() throws Exception {
        // given
        Long labelId = 999L;
        Mockito.doThrow(new LabelNotFoundException("라벨을 찾을 수 없습니다."))
                .when(labelService).deleteLabel(labelId);

        // when & then
        mockMvc.perform(delete("/labels/{id}", labelId))
                .andExpect(status().isNotFound())                 // 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LabelNotFound"))
                .andExpect(jsonPath("$.error.message").value("라벨을 찾을 수 없습니다."));
    }

    /**
     * 이슈에 라벨 추가
     * 성공
     */
    @Test
    void addLabelsToIssue_success() throws Exception {
        // given
        Long issueId = 1L;
        List<Long> labelIds = List.of(10L, 20L);

        IssueLabelsResponse mock = IssueLabelsResponse.builder()
                .id(issueId)
                .labels(List.of(
                        IssueLabelsResponse.LabelSummary.builder().id(10L).name("bug").build(),
                        IssueLabelsResponse.LabelSummary.builder().id(20L).name("feature").build()
                ))
                .build();

        Mockito.when(issueLabelService.addLabelsToIssue(issueId, labelIds))
                .thenReturn(mock);

        String body = "[10, 20]";

        // when & then
        mockMvc.perform(post("/issues/{issueId}/labels", issueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.labels[0].id").value(10))
                .andExpect(jsonPath("$.data.labels[0].name").value("bug"))
                .andExpect(jsonPath("$.data.labels[1].id").value(20))
                .andExpect(jsonPath("$.data.labels[1].name").value("feature"));

        Mockito.verify(issueLabelService).addLabelsToIssue(issueId, labelIds);
    }

    /**
     * 이슈에 라벨 추가
     * 실패: 이슈 조회 실패 시
     */
    @Test
    void addLabelsToIssue_issueNotFound_notfound() throws Exception {
        // given
        Long issueId = 999L;
        String body = "[10, 20]";

        Mockito.when(issueLabelService.addLabelsToIssue(eq(issueId), anyList()))
                .thenThrow(new NotFoundException("Issue not found"));

        // when & then
        mockMvc.perform(post("/issues/{issueId}/labels", issueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())                 // 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("IssueNotFound"))
                .andExpect(jsonPath("$.error.message").value("Issue not found"));
    }

    /**
     * 이슈에 라벨 추가
     * 실패: 없는 라벨 추가 시
     */
    @Test
    void addLabelsToIssue_labelNotFound_notFound() throws Exception {
        // given
        Long issueId = 1L;
        String body = "[10, 999]"; // 999가 없는 라벨이라고 가정

        Mockito.when(issueLabelService.addLabelsToIssue(eq(issueId), anyList()))
                .thenThrow(new LabelNotFoundException("라벨을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(post("/issues/{issueId}/labels", issueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())                  // 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LabelNotFound"))
                .andExpect(jsonPath("$.error.message").value("라벨을 찾을 수 없습니다."));
    }

    /**
     * 이슈에서 라벨 제거
     * 성공
     */
    @Test
    void deleteLabelFromIssue_success() throws Exception {
        // given
        Long issueId = 1L;
        Long labelId = 20L;

        LabelResponse removed = LabelResponse.builder()
                .id(labelId)
                .name("feature")
                .color("#00FF00")
                .description("기능 추가")
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(issueLabelService.deleteLabelFromIssue(eq(issueId), eq(labelId)))
                .thenReturn(removed);

        // when & then
        mockMvc.perform(delete("/issues/{issueId}/labels/{labelId}", issueId, labelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.name").value("feature"));

        Mockito.verify(issueLabelService).deleteLabelFromIssue(issueId, labelId);
    }

    /**
     * 이슈에서 라벨 제거
     * 실패: 이슈-라벨 매핑 없을 시
     */
    @Test
    void deleteLabelFromIssue_issueLabelNotFound_notFound() throws Exception {
        // given
        Long issueId = 1L;
        Long labelId = 999L; // 이 이슈에 매핑되지 않은 라벨이라고 가정

        Mockito.when(issueLabelService.deleteLabelFromIssue(eq(issueId), eq(labelId)))
                .thenThrow(new IssueLabelNotFoundException("이 이슈에 해당 라벨 매핑이 없습니다."));

        // when & then
        mockMvc.perform(delete("/issues/{issueId}/labels/{labelId}", issueId, labelId))
                .andExpect(status().isNotFound())                       // 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("IssueLabelNotFound"))
                .andExpect(jsonPath("$.error.message").value("이 이슈에 해당 라벨 매핑이 없습니다."));

        Mockito.verify(issueLabelService).deleteLabelFromIssue(issueId, labelId);
    }

}