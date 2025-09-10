package com.issueDive.service;

import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.LabelResponse;
import com.issueDive.dto.UpdateLabelRequest;
import com.issueDive.entity.Label;
import com.issueDive.entity.IssueStatus;
import com.issueDive.exception.LabelNotFoundException;
import com.issueDive.exception.ValidationException;
import com.issueDive.repository.LabelRepository;
import com.issueDive.repository.IssueLabelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @InjectMocks
    private LabelService labelService;

    /**
     * 라벨 생성 성공 테스트
     * - 동일 이름 라벨이 존재하지 않을 때,
     * - 라벨이 정상적으로 저장되고,
     * - 반환된 LabelResponse에 입력 정보가 정확히 반영되는지 검증한다.
     */
    @Test
    @DisplayName("라벨 생성 성공")
    void createLabel_success() {
        // given
        CreateLabelRequest request = CreateLabelRequest.builder()
                .name("Bug")
                .color("#FF0000")
                .description("버그 관련 이슈")
                .build();

        Label savedLabel = Label.builder()
                .id(1L)
                .name(request.getName())
                .color(request.getColor())
                .description(request.getDescription())
                .build();

        when(labelRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(labelRepository.save(any(Label.class))).thenReturn(savedLabel);
        when(issueLabelRepository.countByLabelAndIssue_Status(savedLabel, IssueStatus.OPEN)).thenReturn(0L);

        // when
        LabelResponse response = labelService.createLabel(request);

        // then
        assertEquals("Bug", response.getName());
        assertEquals("#FF0000", response.getColor());
        assertEquals("버그 관련 이슈", response.getDescription());
        assertEquals(0L, response.getIssueOpenCount());

        verify(labelRepository).save(any(Label.class));
    }

    /**
     * 라벨 생성 실패 테스트
     * - 동일 이름 라벨이 이미 존재할 때,
     * - ValidationException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("라벨 생성 실패 - 중복 이름")
    void createLabel_duplicateName() {
        // given
        CreateLabelRequest request = CreateLabelRequest.builder()
                .name("Bug")
                .color("#FF0000")
                .description("버그 관련 이슈")
                .build();

        when(labelRepository.existsByNameIgnoreCase(request.getName())).thenReturn(true);

        // when-then
        assertThrows(ValidationException.class, () -> labelService.createLabel(request));

        verify(labelRepository, never()).save(any(Label.class));
    }


    /**
     * 라벨 목록 조회 테스트
     * - 저장된 라벨들이 있을 때,
     * - 각 라벨과 연결된 OPEN 상태 이슈 개수를 함께 조회하여,
     * - LabelResponse 리스트로 정상 변환되는지 검증한다.
     */
    @Test
    @DisplayName("라벨 목록 조회 성공")
    void getAllLabels_success() {
        // given
        // List<Label>이 아닌 List<LabelResponse>를 Mocking
        List<LabelResponse> mockResponses = List.of(
                new LabelResponse(1L, "Bug", "#FF0000", "버그 관련 이슈", 2L),
                new LabelResponse(2L, "Feature", "#00FF00", "기능 추가", 0L)
        );

        // findAll() 대신 새로 만든 findAllWithOpenIssueCount()를 Mocking
        when(labelRepository.findAllWithOpenIssueCount()).thenReturn(mockResponses);

        // when
        List<LabelResponse> responses = labelService.getLabels();

        // then
        assertEquals(2, responses.size());
        assertEquals("Bug", responses.get(0).getName());
        assertEquals(2L,  responses.get(0).getIssueOpenCount());

        assertEquals("Feature", responses.get(1).getName());
        assertEquals(0L,  responses.get(1).getIssueOpenCount());

        verify(labelRepository).findAllWithOpenIssueCount();
    }

    /**
     * 단일 라벨 조회 성공 테스트
     * - 저장된 라벨이 있을 때,
     * - 단일 라벨과 연결된 OPEN 상태 이슈를 함께 조회하여,
     * - LabelResponse로 정상 변환되는지 검증한다.
     */
    @Test
    @DisplayName("단일 라벨 조회 성공")
    void getLabel_success(){
        //given
        Label label = Label.builder()
                .id(1L)
                .name("Bug")
                .color("#FF0000")
                .description("버그 관련 이슈")
                .build();

        when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
        when(issueLabelRepository.countByLabelAndIssue_Status(label, IssueStatus.OPEN)).thenReturn(2L);

        //when
        LabelResponse response = labelService.getLabel(1L);

        //then
        assertEquals("Bug", response.getName());
        assertEquals(2L, response.getIssueOpenCount());
        assertEquals("#FF0000", response.getColor());
        assertEquals("버그 관련 이슈",  response.getDescription());

        verify(labelRepository).findById(1L);
    }

    /**
     * 단일 라벨 조회 실패 테스트
     * - 존재하지 않는 라벨 ID로 조회할 때,
     * - LabelNotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("단일 라벨 조회 실패")
    void getLabel_notFound(){
        //given
        Long invalidId = 999L;
        when(labelRepository.findById(999L)).thenReturn(Optional.empty());

        //when-then
        assertThrows(LabelNotFoundException.class, () -> labelService.getLabel(invalidId));
    }

    /**
     * 라벨 수정 성공 테스트
     * - 기존 라벨이 존재할 때,
     * - 새로운 값을 전달하면,
     * - 정상적으로 수정된 LabelResponse가 반환되는지 검증한다.
     */
    @Test
    @DisplayName("라벨 수정 성공")
    void updateLabel_success() {
        //given
        Long id = 1L;
        Label label = Label.builder()
                .id(id)
                .name("Bug")
                .color("#FF0000")
                .description("버그 관련 이슈")
                .build();

        UpdateLabelRequest request = UpdateLabelRequest.builder()
                .name("Fixed")
                .color("#00FF00")
                .description("수정됨")
                .build();

        when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
        when(labelRepository.existsByNameIgnoreCase("Fixed")).thenReturn(false);
        when(labelRepository.save(any(Label.class))).thenReturn(label);
        when(issueLabelRepository.countByLabelAndIssue_Status(label, IssueStatus.OPEN)).thenReturn(1L);

        //when
        LabelResponse response = labelService.updateLabel(id, request);

        //then
        assertEquals("Fixed", response.getName());
        assertEquals("#00FF00", response.getColor());
        assertEquals("수정됨", response.getDescription());
        assertEquals(1L, response.getIssueOpenCount());

        verify(labelRepository).save(any(Label.class));
    }

    /**
     * 라벨 수정 실패 테스트
     * - 존재하지 않는 라벨 ID로 수정 시도할 때,
     * - LabelNotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("라벨 수정 실패 테스트")
    void updateLabel_notFound() {
        //given
        Long invalidId = 999L;
        UpdateLabelRequest request = UpdateLabelRequest.builder()
                .name("Fixed")
                .color("#00FF00")
                .description("수정됨")
                .build();

        when(labelRepository.findById(invalidId)).thenReturn(Optional.empty());

        // when-then
        assertThrows(LabelNotFoundException.class, () -> labelService.updateLabel(invalidId, request));
    }

    /**
     * 라벨 수정 실패 테스트
     * - 변경하려는 이름이 이미 다른 라벨에 존재할 때,
     * - ValidationException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("라벨 수정 실패 - 중복 이름")
    void updateLabel_duplicateName() {
        // given
        Long labelId = 1L;
        Label existing = Label.builder()
                .id(labelId)
                .name("Bug")
                .color("#FF0000")
                .description("버그 관련 이슈")
                .build();

        UpdateLabelRequest request = UpdateLabelRequest.builder()
                .name("Feature") // 이미 존재하는 다른 라벨 이름이라고 가정
                .color("#00FF00")
                .description("수정됨")
                .build();

        when(labelRepository.findById(labelId)).thenReturn(Optional.of(existing));
        when(labelRepository.existsByNameIgnoreCase("Feature")).thenReturn(true);

        // when-then
        assertThrows(ValidationException.class, () -> labelService.updateLabel(labelId, request));

        verify(labelRepository, never()).save(any(Label.class));
    }

    /**
     * 라벨 삭제 성공 테스트
     * - 존재하는 라벨 ID로 삭제를 요청할 때,
     * - 연관된 issue_label 매핑이 먼저 제거되고,
     * - 라벨이 정상적으로 삭제되는지 검증한다.
     */
    @Test
    @DisplayName("라벨 삭제 성공")
    void deleteLabel_success() {
        // given
        Long labelId = 1L;

        when(labelRepository.existsById(labelId)).thenReturn(true);
        doNothing().when(issueLabelRepository).deleteByLabelId(labelId);
        doNothing().when(labelRepository).deleteById(labelId);

        // when
        assertDoesNotThrow(() -> labelService.deleteLabel(labelId));

        // then
        verify(issueLabelRepository).deleteByLabelId(labelId);
        verify(labelRepository).deleteById(labelId);
    }

    /**
     * 라벨 삭제 실패 테스트
     * - 존재하지 않는 라벨 ID로 삭제를 요청할 때,
     * - LabelNotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("라벨 삭제 실패 - 라벨 없음")
    void deleteLabel_notFound() {
        // given
        Long invalidId = 999L;
        when(labelRepository.existsById(invalidId)).thenReturn(false);

        // when-then
        assertThrows(LabelNotFoundException.class, () -> labelService.deleteLabel(invalidId));

        verify(labelRepository, never()).deleteById(anyLong());
        verify(issueLabelRepository, never()).deleteByLabelId(anyLong());
    }
}
