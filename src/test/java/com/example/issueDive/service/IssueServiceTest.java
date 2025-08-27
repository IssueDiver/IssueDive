package com.example.issueDive.service;

import com.example.issueDive.dto.CreateIssueRequest;
import com.example.issueDive.dto.IssueFilterRequest;
import com.example.issueDive.dto.IssueResponse;
import com.example.issueDive.dto.UpdateIssueRequest;
import com.example.issueDive.entity.Issue;
import com.example.issueDive.entity.IssueStatus;
import com.example.issueDive.entity.User;
import com.example.issueDive.exception.NotFoundException;
import com.example.issueDive.exception.ValidationException;
import com.example.issueDive.repository.IssueRepository;
import com.example.issueDive.repository.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;

@ExtendWith(MockitoExtension.class) // DB나 외부 시스템에 독립적이도록 Mocking 활용
public class IssueServiceTest {

    // @Mock private JPAQueryFactory jpaQueryFactory;
    @Mock private IssueRepository issueRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private IssueService issueService;

    /**
     * 이슈 생성 성공 테스트
     * - 유효한 작성자(authorId)와 담당자(assigneeId)가 존재할 때,
     * - 서비스가 정상적으로 Issue 객체를 생성해 저장하며,
     * - 반환된 IssueResponse에 입력 정보가 정확히 반영되는지 검증한다.
     */
    @Test
    void createIssue_success() {
        // given
        Long authorId = 1L;
        Long assigneeId = 2L;

        CreateIssueRequest request = new CreateIssueRequest("제목", "설명", assigneeId);

        User author = new User();
        author.setId(authorId);
        User assignee = new User();
        assignee.setId(assigneeId);

        // 사용자 조회 및 이슈 저장 동작 모방
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0)); // 파라미터 Issue 객체 그대로 반환하도록 설정 (실제 저장 시뮬레이션)

        // when
        IssueResponse response = issueService.createIssue(request, authorId);

        // then
        assertEquals("제목", response.title());
        assertEquals(authorId, response.authorId());
        assertEquals(assigneeId, response.assigneeId());
        verify(issueRepository).save(any(Issue.class)); // 이슈 저장 메서드가 호출되었는지 검증

    }

    /**
     * 이슈 생성 실패 테스트 - 작성자(Author: User) 미존재 예외처리
     * - 존재하지 않는 작성자 ID로 이슈 생성 시도 시,
     * - NotFoundException 예외를 발생시키는지 검증한다.
     */
    @Test
    void createIssue_authorNotFound() {

        // given
        Long authorId = 99L;
        CreateIssueRequest request = new CreateIssueRequest("제목", "설명", null);
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        // when-then
        assertThrows(NotFoundException.class, () -> issueService.createIssue(request, authorId));

    }

    /**
     * 다중 조건 필터와 페이징이 적용된 이슈 조회 테스트
     * -> 통합 테스트로 검증 (QueryDSL 코드를 모킹하는 것은 비효율적)
     * --> IssueDiveApplicationTests.java 체크
     */
//    @Test
//    void getFilteredIssues_withMultipleFilters_returnsPagedResults() {
//    }

    /**
     * 이슈 조회 성공 테스트
     * - 존재하는 이슈 ID로 조회하면,
     * - 해당 이슈 정보가 정확하게 IssueResponse로 변환되어 반환되는지 확인한다.
     */
    @Test
    void getIssue_success() {
        // given
        User author = User.builder().id(1L).build();
        Long issueId = 10L;
        Issue issue = Issue.builder()
                .id(issueId)
                .title("테스트")
                .status(IssueStatus.OPEN)
                .author(author)
                .build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        // when
        IssueResponse res = issueService.getIssue(issueId);

        // then
        assertEquals(issueId, res.id());
        assertEquals("테스트", res.title());
        assertEquals("OPEN", res.status());
    }

    /**
     * 이슈 조회 실패 테스트 - 존재하지 않는 이슈 조회 예외
     * - 존재하지 않는 이슈 ID로 조회 시,
     * - NotFoundException 예외 발생 여부를 검증한다.
     */
    @Test
    void getIssue_notFound() {
        // given
        Long invalidId = 999L;
        when(issueRepository.findById(invalidId)).thenReturn(Optional.empty());
        // when-then
        assertThrows(NotFoundException.class, () -> issueService.getIssue(invalidId));
    }

    /**
     * 이슈 수정 성공 테스트
     * - 기존 이슈가 존재하고, 새로운 담당자가 유효할 때,
     * - 수정 요청 내용을 반영하여 정상적으로 이슈가 수정되는지 검증한다.
     */
    @Test
    void updateIssue_success() {

        // given
        Long issueId = 10L;
        Long authorId = 1L;
        User author = User.builder().id(authorId).build();
        Issue existing = Issue.builder().id(issueId).title("원래 제목").description("원래 설명").author(author).build();

        Long newAssigneeId = 3L;
        UpdateIssueRequest request = new UpdateIssueRequest("수정된 제목", "수정된 설명", newAssigneeId);
        User newAssignee = User.builder().id(newAssigneeId).build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(newAssigneeId)).thenReturn(Optional.of(newAssignee));
        when(issueRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // when
        IssueResponse updated = issueService.updateIssue(issueId, request);

        // then
        assertEquals("수정된 제목", updated.title());
        assertEquals("수정된 설명", updated.description());
        assertEquals(newAssigneeId, updated.assigneeId());

    }

    /**
     * 이슈 수정 실패 테스트 - 수정 대상 이슈 미존재 예외
     * - 수정 시도하는 이슈 ID가 없을 때,
     * - NotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    void updateIssue_notFound() {
        // given
        when(issueRepository.findById(anyLong())).thenReturn(Optional.empty());
        UpdateIssueRequest request = new UpdateIssueRequest("제목", "설명", null);

        // when-then
        assertThrows(NotFoundException.class, () -> issueService.updateIssue(999L, request));
    }

    /**
     * 이슈 삭제 성공 테스트
     * - 삭제 요청한 이슈 ID가 존재할 때,
     * - 정상적으로 이슈 삭제가 수행되는지 검증한다.
     */
    @Test
    void deleteIssue_success() {
        // given
        Long issueId = 10L;
        when(issueRepository.existsById(issueId)).thenReturn(true);
        doNothing().when(issueRepository).deleteById(issueId);

        // when
        assertDoesNotThrow(() -> issueService.deleteIssue(issueId));

        // then
        verify(issueRepository).deleteById(issueId); // 호출 여부 체크
    }

    /**
     * 이슈 상태 변경 테스트
     * 정상 케이스: OPEN 또는 CLOSED 상태로 변경 시 정상 응답 확인
     */
    @Test
    void changeIssueStatus_validStatus_success() {
        // given
        Issue issue = Issue.builder()
                .id(1L)
                .status(IssueStatus.OPEN)
                .author(User.builder().id(1L).build())
                .build();

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //when
        IssueResponse response = issueService.changeIssueStatus(1L, "CLOSED");

        // then
        assertEquals("CLOSED", response.status());
        verify(issueRepository).save(issue);
    }

    /**
     * 이슈 상태 변경 테스트
     * 예외 케이스1: 존재하지 않는 이슈 ID 요청 시 NotFoundException 발생 확인
     */
    @Test
    void changeIssueStatus_issueNotFound() {
        // given
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        // when-then
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> issueService.changeIssueStatus(99L, "OPEN"));

        assertEquals("Issue not found", ex.getMessage());
    }

    /**
     * 이슈 상태 변경 테스트
     * 예외 케이스2: 유효하지 않은 상태 값 요청 시 ValidationException 발생 확인
     */
    @Test
    void changeIssueStatus_invalidStatus_throwsValidationException() {
        // given
        Issue issue = Issue.builder()
                .id(1L)
                .status(IssueStatus.OPEN)
                .author(User.builder().id(1L).build())
                .build();

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        // when-then
        ValidationException ex = assertThrows(ValidationException.class,
                () -> issueService.changeIssueStatus(1L, "INVALID_STATUS"));

        assertTrue(ex.getMessage().contains("status must be either OPEN or CLOSED"));
    }

    /**
     * 이슈 삭제 실패 테스트 - 삭제 대상 이슈 미존재 예외
     * - 삭제 요청한 이슈 ID가 존재하지 않을 때,
     * - NotFoundException 예외가 발생하는지 검증한다.
     */
    @Test
    void deleteIssue_notFound() {
        // given
        Long invalidId = 999L;
        when(issueRepository.existsById(invalidId)).thenReturn(false);

        // when-then
        assertThrows(NotFoundException.class, () -> issueService.deleteIssue(invalidId));
    }

}
