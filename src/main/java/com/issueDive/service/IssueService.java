package com.issueDive.service;

import com.issueDive.dto.CreateIssueRequest;
import com.issueDive.dto.IssueFilterRequest;
import com.issueDive.dto.IssueResponse;
import com.issueDive.dto.UpdateIssueRequest;
import com.issueDive.entity.*;
import com.issueDive.exception.ErrorCode;
import com.issueDive.exception.NotFoundException;
import com.issueDive.exception.ValidationException;
import com.issueDive.repository.IssueLabelRepository;
import com.issueDive.repository.IssueRepository;
import com.issueDive.repository.LabelRepository;
import com.issueDive.repository.UserRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final JPAQueryFactory queryFactory;
    private final QIssue qIssue = QIssue.issue;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository; // 작성자/담당자 유효성 검증용
    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final QIssueLabel qIssueLabel = QIssueLabel.issueLabel;

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
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setAuthor(author);
        issue.setAssignee(assignee);
        issue.setStatus(IssueStatus.OPEN);

        //라벨 매핑 추가
        if (request.labels() != null && !request.labels().isEmpty()) {
            List<Label> labels = labelRepository.findAllById(request.labels());
            issue.getLabels().addAll(labels);
        }

        Issue saved = issueRepository.save(issue);
        return toResponse(saved);
    }

    /**
     * 다중 조회 (필터링, 페이징)
     * @param filter status, authorId, labelIds, page, size, sort, order
     * @return 필터링, 페이징 등 적용된 이슈 dto 리스트(페이지)
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getFilteredIssues(IssueFilterRequest filter) {
        BooleanBuilder builder = createFilterBuilder(filter);
        Pageable pageable = createPageable(filter);
        OrderSpecifier<?> orderSpecifier = getSortOrder(filter.sort(), filter.order());

        // 1. 조건에 맞는 이슈 ID 목록을 먼저 조회 (페이징 적용)
        List<Long> ids = queryFactory
                .select(qIssue.id)
                .from(qIssue)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifier)
                .fetch();

        if (ids.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 2. ID 목록을 사용해 이슈를 조회하면서, 연관된 issueLabels와 그 안의 label을 fetch join으로 함께 가져옴
        List<Issue> issues = queryFactory
                .selectFrom(qIssue)
                .leftJoin(qIssue.issueLabels, qIssueLabel).fetchJoin() // issue -> issueLabels 조인
                .leftJoin(qIssueLabel.label).fetchJoin()               // issueLabels -> label 조인
                .where(qIssue.id.in(ids))
                .orderBy(orderSpecifier)
                .fetch();

        // 중복된 이슈를 제거하고 ID 순서대로 정렬
        List<Issue> distinctIssues = ids.stream()
                .flatMap(id -> issues.stream().filter(issue -> issue.getId().equals(id)))
                .distinct()
                .collect(Collectors.toList());

        // 3. 전체 카운트 조회
        long total = queryFactory
                .select(qIssue.id)
                .from(qIssue)
                .where(builder)
                .fetchCount();

        List<IssueResponse> dtoList = distinctIssues.stream().map(this::toResponse).toList();
        return new PageImpl<>(dtoList, pageable, total);
    }


    // 동적 정렬을 위한 헬퍼 메소드
    private OrderSpecifier<?> getSortOrder(String sortProperty, String orderDirection) {
        Order direction = "desc".equalsIgnoreCase(orderDirection) ? Order.DESC : Order.ASC;
        if ("createdAt".equalsIgnoreCase(sortProperty)) {
            return new OrderSpecifier<>(direction, qIssue.createdAt);
        }
        if ("updatedAt".equalsIgnoreCase(sortProperty)) {
            return new OrderSpecifier<>(direction, qIssue.updatedAt);
        }
        return new OrderSpecifier<>(Order.DESC, qIssue.createdAt);
    }

    // 필터 조건을 생성하는 헬퍼 메소드
    private BooleanBuilder createFilterBuilder(IssueFilterRequest filter) {
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.status() != null && !filter.status().isEmpty()) {
            builder.and(qIssue.status.eq(IssueStatus.valueOf(filter.status().toUpperCase())));
        }
        if (filter.authorId() != null) {
            builder.and(qIssue.author.id.eq(filter.authorId()));
        }
        if (filter.assigneeId() != null) {
            builder.and(qIssue.assignee.id.eq(filter.assigneeId()));
        }
        // 라벨 ID로 필터링
        if (filter.labelIds() != null && !filter.labelIds().isEmpty()) {
            builder.and(qIssue.issueLabels.any().label.id.in(filter.labelIds()));
        }
        // 텍스트로 검색 (제목, 작성자, 담당자, 라벨 이름)
        if (filter.query() != null && !filter.query().isBlank()) {
            String searchQuery = filter.query();
            builder.and(
                    qIssue.title.containsIgnoreCase(searchQuery)
                            .or(qIssue.author.username.containsIgnoreCase(searchQuery))
                            .or(qIssue.assignee.username.containsIgnoreCase(searchQuery))
                            .or(qIssue.issueLabels.any().label.name.containsIgnoreCase(searchQuery))
            );
        }
        return builder;
    }

    // Pageable 객체를 생성하는 헬퍼 메소드
    private Pageable createPageable(IssueFilterRequest filter) {
        int page = filter.page() != null ? filter.page() : 0;
        int size = filter.size() != null ? filter.size() : 10;
        return PageRequest.of(page, size);
    }

    /**
     * 단일 조회
     * @param id 조회할 이슈 id
     * @return 조회한 이슈 dto
     */
    @Transactional(readOnly = true)
    public IssueResponse getIssue(Long id) {
        Issue issue = issueRepository.findWithLabelsById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        return toResponse(issue);
    }

    /**
     * 수정
     * @param id 수정할 이슈 id
     * @param request (선택적으로) title, description, assigneeId, labelIds
     * @return 수정한 이슈 dto
     */
    @Transactional
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

        if (request.labelIds() != null) {
            // 1. 기존 라벨 연결을 DB에서 전부 삭제
            issueLabelRepository.deleteByIssueId(issue.getId());

            // 2. 새로운 라벨 목록 조회
            List<Label> newLabels = labelRepository.findAllById(request.labelIds());

            // 3. 새로운 연결 객체 생성
            List<IssueLabel> newIssueLabels = newLabels.stream()
                    .map(label -> new IssueLabel(issue, label))
                    .collect(Collectors.toList());

            // 4. 새로 만든 연결을 DB에 전부 저장
            issueLabelRepository.saveAll(newIssueLabels);

            // 5. 메모리의 issue 객체 상태도 동기화
            issue.setIssueLabels(newIssueLabels);
        }

        return toResponse(issue);
    }

    /**
     * 이슈 상태 변경
     * @param id 상태 변경할 Issue ID
     * @param status 변경할 상태 (OPEN, IN_PROGRESS, CLOSED)
     * @return 상태가 변경된 IssueResponse
     */
    public IssueResponse changeIssueStatus(Long id, String status) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found"));

        IssueStatus newStatus;
        try {
            newStatus = IssueStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ErrorCode.InvalidStatus, "status must be one of OPEN, IN_PROGRESS, or CLOSED");
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
        List<Long> labelIds = issue.getIssueLabels().stream()
                .map(issueLabel -> issueLabel.getLabel().getId())
                .toList();

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
