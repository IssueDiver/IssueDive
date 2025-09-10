package com.issueDive.service;

import com.issueDive.dto.IssueLabelsResponse;
import com.issueDive.entity.Issue;
import com.issueDive.entity.Label;
import com.issueDive.entity.IssueLabel;
import com.issueDive.entity.IssueLabelId;
import com.issueDive.exception.IssueLabelNotFoundException;
import com.issueDive.exception.LabelNotFoundException;
import com.issueDive.exception.NotFoundException;
import com.issueDive.repository.IssueLabelRepository;
import com.issueDive.repository.IssueRepository;
import com.issueDive.repository.LabelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueLabelServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @InjectMocks
    private IssueLabelService issueLabelService;

    /**
     * 이슈에 라벨 추가 성공 테스트
     * - 존재하는 이슈와 라벨 ID를 전달했을 때,
     * - 새로운 라벨이 정상적으로 매핑되어 저장되고,
     * - 반환된 IssueLabelService에 반영되는지 검증한다.
     */
    @Test
    @DisplayName("이슈에 라벨 추가 성공")
    void addLabelToIssue_success(){
        //given
        Long issueId = 1L;
        Long labelId = 2L;

        Issue issue = Issue.builder().id(issueId).title("테스트 이슈").build();
        Label label = Label.builder().id(labelId).name("Bug").color("#FF0000").build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(labelRepository.findAllById(List.of(labelId))).thenReturn(List.of(label));
        when(issueLabelRepository.existsByIssueAndLabel(issue, label)).thenReturn(false);
        when(issueLabelRepository.save(any(IssueLabel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(issueLabelRepository.findAllByIssue(issue))
                .thenReturn(List.of(IssueLabel.builder().issue(issue).label(label).build()));

        //when
        IssueLabelsResponse response = issueLabelService.addLabelsToIssue(issueId, List.of(labelId));

        //then
        assertEquals(issueId, response.getId());
        assertEquals(1,  response.getLabels().size());
        assertEquals(labelId, response.getLabels().get(0).getId());
        assertEquals("Bug", response.getLabels().get(0).getName());
        assertEquals("#FF0000", response.getLabels().get(0).getColor());

        verify(issueRepository).findById(issueId);
        verify(labelRepository).findAllById(List.of(labelId));
        verify(issueLabelRepository).save(any(IssueLabel.class));
    }

    /**
     * 이슈에 라벨 추가 실패 테스트
     * - 존재하지 않는 이슈 ID를 전달했을 때,
     * - NotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("이슈에 라벨 추가 실패 - 이슈 없음")
    void addLabelToIssue_issueNotFound(){
        //given
        Long invalidIssueId = 999L;
        List<Long> labelIds = List.of(1L);

        when(issueRepository.findById(invalidIssueId)).thenReturn(Optional.empty());

        //when-then
        assertThrows(NotFoundException.class, () -> issueLabelService.addLabelsToIssue(invalidIssueId, labelIds));

        verify(issueRepository).findById(invalidIssueId);
        verify(labelRepository, never()).findAllById(any());
        verify(issueLabelRepository, never()).save(any());
    }

    /**
     * 이슈에 라벨 추가 실패 테스트
     * - 존재하지 않는 라벨 ID를 전달했을 때,
     * - LabelNotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("이슈에 라벨 추가 실패 - 라벨 없음")
    void addLabelToIssue_labelNotFound(){
        //given
        Long issueId = 1L;
        Long invalidLabelId = 999L;
        List<Long> labelIds = List.of(invalidLabelId);

        Issue issue = Issue.builder().id(issueId).title("테스트 이슈").build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(labelRepository.findAllById(labelIds)).thenReturn(List.of());

        //when-then
        assertThrows(LabelNotFoundException.class, () -> issueLabelService.addLabelsToIssue(issueId, labelIds));

        verify(issueRepository).findById(issueId);
        verify(labelRepository).findAllById(List.of(invalidLabelId));
        verify(issueLabelRepository, never()).save(any());
    }

    /**
     * 이슈에 연결된 라벨 조회 성공 테스트
     * - 특정 이슈에 여러 라벨이 매핑되어 있을 때,
     * - findLabelsByIssue가 해당 라벨들을 정확히 반환하는지 검증한다.
     */
    @Test
    @DisplayName("이슈에 연결된 라벨 조회 성공")
    void findLabelsByIssue_success(){
        //given
        Issue issue = Issue.builder().id(1L).title("테스트 이슈").build();

        Label label1 = Label.builder().id(10L).name("Bug").color("#FF0000").build();
        Label label2 = Label.builder().id(11L).name("Feature").color("#00FF00").build();

        IssueLabel mapping1 = IssueLabel.builder().issue(issue).label(label1).build();
        IssueLabel mapping2 = IssueLabel.builder().issue(issue).label(label2).build();

        when(issueLabelRepository.findAllByIssue(issue)).thenReturn(List.of(mapping1, mapping2));

        //when
        List<Label> labels = issueLabelService.findLabelsByIssue(issue);

        //then
        assertEquals(2, labels.size());
        assertTrue(labels.contains(label1));
        assertTrue(labels.contains(label2));

        verify(issueLabelRepository).findAllByIssue(issue);
    }

    /**
     * 이슈에서 라벨 제거 성공 테스트
     * - 존재하는 이슈와 라벨이 매핑되어 있을 때,
     * - 삭제 후 반환된 IssueLabelsResponse에 해당 라벨이 빠졌는지 검증한다.
     */
    @Test
    @DisplayName("이슈에서 라벨 제거 성공")
    void deleteLabelsByIssue_success(){
        //given
        Long issueId = 1L;
        Long deleteLabelId = 10L;

        Issue issue = Issue.builder().id(issueId).title("테스트 이슈").build();
        Label label1 = Label.builder().id(deleteLabelId).name("Bug").color("#FF0000").build();
        Label label2 = Label.builder().id(11L).name("Feature").color("#00FF00").build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(labelRepository.findById(deleteLabelId)).thenReturn(Optional.of(label1));
        when(issueLabelRepository.existsByIssueAndLabel(issue, label1)).thenReturn(true);

        // 삭제 후에는 label2만 남아 있다고 가정
        when(issueLabelRepository.findAllByIssue(issue))
                .thenReturn(List.of(IssueLabel.builder().issue(issue).label(label2).build()));

        //when
        IssueLabelsResponse response = issueLabelService.deleteLabelFromIssue(issueId, deleteLabelId);

        //then
        assertEquals(issueId, response.getId());
        assertEquals(1, response.getLabels().size());
        assertEquals("Feature", response.getLabels().get(0).getName());
        assertEquals("#00FF00", response.getLabels().get(0).getColor());

        verify(issueRepository).findById(issueId);
        verify(labelRepository).findById(deleteLabelId);
        verify(issueLabelRepository).deleteByIssueAndLabel(issue, label1);
    }

    /**
     * 이슈에서 라벨 제거 실패 테스트
     * - 존재하지 않는 이슈 ID를 전달했을 때,
     * - NotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("이슈에서 라벨 제거 실패 - 이슈 없음")
    void deleteLabelFromIssue_issueNotFound() {
        // given
        Long invalidIssueId = 999L;
        when(issueRepository.findById(invalidIssueId)).thenReturn(Optional.empty());

        // when-then
        assertThrows(NotFoundException.class,
                () -> issueLabelService.deleteLabelFromIssue(invalidIssueId, 10L));

        verify(issueRepository).findById(invalidIssueId);
        verify(labelRepository, never()).findById(any());
        verify(issueLabelRepository, never()).deleteByIssueAndLabel(any(), any());
    }

    /**
     * 이슈에서 라벨 제거 실패 테스트
     * - 존재하지 않는 라벨 ID를 전달했을 때,
     * - NotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("이슈에서 라벨 제거 실패 - 라벨 없음")
    void deleteLabelFromIssue_labelNotFound() {
        // given
        Long issueId = 1L;
        Long invalidLabelId = 999L;

        Issue issue = Issue.builder().id(issueId).title("테스트 이슈").build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(labelRepository.findById(invalidLabelId)).thenReturn(Optional.empty());

        // when-then
        assertThrows(NotFoundException.class,
                () -> issueLabelService.deleteLabelFromIssue(issueId, invalidLabelId));

        verify(issueRepository).findById(issueId);
        verify(labelRepository).findById(invalidLabelId);
        verify(issueLabelRepository, never()).deleteByIssueAndLabel(any(), any());
    }

    /**
     * 이슈에서 라벨 제거 실패 테스트
     * - 이슈와 라벨은 존재하지만,
     * - 두 엔티티가 연결되어 있지 않을 때,
     * - IssueLabelNotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    @DisplayName("이슈에서 라벨 제거 실패 - 매핑 없음")
    void deleteLabelFromIssue_mappingNotFound() {
        // given
        Long issueId = 1L;
        Long labelId = 10L;

        Issue issue = Issue.builder().id(issueId).title("테스트 이슈").build();
        Label label = Label.builder().id(labelId).name("Bug").build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));
        when(issueLabelRepository.existsByIssueAndLabel(issue, label)).thenReturn(false);

        // when-then
        assertThrows(IssueLabelNotFoundException.class,
                () -> issueLabelService.deleteLabelFromIssue(issueId, labelId));

        verify(issueRepository).findById(issueId);
        verify(labelRepository).findById(labelId);
        verify(issueLabelRepository).existsByIssueAndLabel(issue, label);
        verify(issueLabelRepository, never()).deleteByIssueAndLabel(any(), any());
    }
}
