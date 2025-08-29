package com.issueDive.service;

import com.issueDive.dto.CreateIssueRequest;
import com.issueDive.dto.IssueFilterRequest;
import com.issueDive.dto.IssueResponse;
import com.issueDive.dto.UpdateIssueRequest;
import com.issueDive.entity.*;
import com.issueDive.entity.Issue;
import com.issueDive.entity.IssueStatus;
import com.issueDive.entity.Label;
import com.issueDive.entity.User;
import com.issueDive.exception.ErrorCode;
import com.issueDive.exception.NotFoundException;
import com.issueDive.exception.ValidationException;
import com.issueDive.repository.IssueRepository;
import com.issueDive.repository.UserRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final JPAQueryFactory queryFactory;
    private final QIssue qIssue = QIssue.issue;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository; // 작성자/담당자 유효성 검증용

    /**
     * Issue 생성
     * @param request title, description, assignee(uid)
     * @param authorId 작성자 user id
     * @return 생성된 이슈 dto
     */
    public IssueResponse createIssue(CreateIssueRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new NotFoundException("Assignee not found"));
        }

        Issue issue = new Issue();
        issue.setTitle(request.title()); // not null ?
        issue.setDescription(request.description());
        issue.setAuthor(author);
        issue.setAssignee(assignee);
        issue.setStatus(IssueStatus.OPEN);

        Issue saved = issueRepository.save(issue);
        return toResponse(saved);
    }

    /**
     * 다중 조회 (필터링, 페이징)
     * @param filter status, authorId, labelIds, page, size, sort, order
     * @return 필터링, 페이징 등 적용된 이슈 dto 리스트(페이지)
     */
    public Page<IssueResponse> getFilteredIssues(IssueFilterRequest filter) {
        // QueryDSL이나 Criteria API 등으로 동적 쿼리 작성

        // 동적 조건
        BooleanBuilder builder = new BooleanBuilder();
        if (filter.status() != null) builder.and(qIssue.status.eq(IssueStatus.valueOf(filter.status().toUpperCase())));
        if (filter.authorId()!=null) builder.and(qIssue.author.id.eq(filter.authorId()));
        if (filter.assigneeId()!=null) builder.and(qIssue.assignee.id.eq(filter.assigneeId()));
        if (filter.labelIds()!=null && !filter.labelIds().isEmpty()) builder.and(qIssue.labels.any().id.in(filter.labelIds()));

        // 페이징 객체
        int page = filter.page();
        int size = filter.size();
        Sort sort = Sort.by(Sort.Direction.fromString(filter.order()), filter.sort());
        Pageable pageable = PageRequest.of(page, size, sort);

        // 쿼리 실행
        List<Issue> issues = queryFactory
                .selectFrom(qIssue)
                .leftJoin(qIssue.labels).fetchJoin() // .leftJoin(qIssue.labels, QLabel.label).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy("asc".equalsIgnoreCase(filter.order())?qIssue.createdAt.asc() : qIssue.createdAt.desc())
                .fetch();

        long total = queryFactory.selectFrom(qIssue).where(builder).fetchCount(); // 전체 카운트 조회 (페이징 정보용)
        List<IssueResponse> dtoList = issues.stream().map(this::toResponse).toList(); // Entity -> DTO 변환

        // 페이지 결과 반환
        return new PageImpl<>(dtoList, pageable, total);
    }

    /**
     * 단일 조회
     * @param id 조회할 이슈 id
     * @return 조회한 이슈 dto
     */
    public IssueResponse getIssue(Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        return toResponse(issue);
    }

    /**
     * 수정
     * @param id 수정할 이슈 id
     * @param request (선택적으로) title, description, assignee(uid)
     * @return 수정한 이슈 dto
     */
    public IssueResponse updateIssue(Long id, UpdateIssueRequest request) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found"));

        if (request.title() != null) issue.setTitle(request.title());
        if (request.description() != null) issue.setDescription(request.description());

        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new NotFoundException("Assignee not found"));
            issue.setAssignee(assignee);
        }

        Issue updated = issueRepository.save(issue);
        return toResponse(updated);
    }

    /**
     * 이슈 상태 변경
     * @param id 상태 변경할 Issue ID
     * @param status 변경할 상태 (OPEN, CLOSED)
     * @return 상태가 변경된 IssueResponse
     */
    public IssueResponse changeIssueStatus(Long id, String status) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found"));

        IssueStatus newStatus;
        try {
            newStatus = IssueStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ErrorCode.InvalidStatus, "status must be either OPEN or CLOSED");
        }

        issue.setStatus(newStatus);
        Issue updated = issueRepository.save(issue);
        return toResponse(updated);
    }

    /**
     * 삭제
     * @param id 삭제할 이슈 id
     */
    public void deleteIssue(Long id) {
        if (!issueRepository.existsById(id)) {
            throw new NotFoundException("Issue not found");
        }
        issueRepository.deleteById(id);
    }

    private IssueResponse toResponse(Issue issue) {
        List<Long> labelIds = issue.getLabels().stream().map(Label::getId).toList();

        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus().name(),
                issue.getAuthor().getId(),
                issue.getAssignee() != null ? issue.getAssignee().getId() : null,
                labelIds,
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}
