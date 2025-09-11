package com.issueDive.service;

import com.issueDive.dto.*;
import com.issueDive.entity.*;
import com.issueDive.exception.ErrorCode;
import com.issueDive.exception.NotFoundException;
import com.issueDive.exception.ValidationException;
import com.issueDive.repository.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final JPAQueryFactory queryFactory;
    private final QIssue qIssue = QIssue.issue;
    private final QComment qComment = QComment.comment;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository; // 작성자/담당자 유효성 검증용
    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final IssueAssigneeRepository issueAssigneeRepository;
    private final QIssueLabel qIssueLabel = QIssueLabel.issueLabel;

    /**
     * Issue 생성
     * @param request title, description, assignee(uid)
     * @param authorId 작성자 user id
     * @return 생성된 이슈 dto
     */
    @Caching(evict = {
            @CacheEvict(value = "issues", allEntries = true),
            @CacheEvict(value = "issue_navigation", allEntries = true)
    }) // 'issues'와 'issue_navigation' 캐시를 비웁니다.
    public IssueResponse createIssue(CreateIssueRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Issue issue = new Issue();
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setAuthor(author);
        issue.setStatus(IssueStatus.OPEN);

        // 다중 담당자 매핑
        if (request.assigneeIds() != null && !request.assigneeIds().isEmpty()) {
            List<User> assignees = userRepository.findAllById(request.assigneeIds());
            Set<IssueAssignee> issueAssignees = assignees.stream()
                    .map(assignee -> new IssueAssignee(issue, assignee))
                    .collect(Collectors.toSet());
            issue.setIssueAssignees(issueAssignees);
        }

        // 다중 라벨 매핑
        if (request.labels() != null && !request.labels().isEmpty()) {
            List<Label> labels = labelRepository.findAllById(request.labels());
            Set<IssueLabel> issueLabels = labels.stream()
                    .map(label -> new IssueLabel(issue, label))
                    .collect(Collectors.toSet());
            issue.setIssueLabels(issueLabels);
        }

        Issue saved = issueRepository.save(issue);

        Issue result = issueRepository.findWithDetailsById(saved.getId())
                .orElseThrow(() -> new NotFoundException("Failed to fetch created issue with details"));
        return toResponse(result);
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

        List<Long> ids;

        // 'commentCount' 정렬일 경우, 별도의 최적화된 쿼리를 사용합니다.
        if ("commentCount".equalsIgnoreCase(filter.sort())) {
            Order direction = "desc".equalsIgnoreCase(filter.order()) ? Order.DESC : Order.ASC;

            ids = queryFactory
                    .select(qIssue.id)
                    .from(qIssue)
                    .leftJoin(qIssue.comments, qComment) // comment 테이블과 JOIN
                    .where(builder)
                    .groupBy(qIssue.id) // issue ID로 그룹화
                    .orderBy(qComment.count().as("comment_count").castToNum(Long.class).desc()) // 댓글 개수로 정렬
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();
        }
        else { // 그 외의 정렬은 기존 방식을 사용

            // 1. 조건에 맞는 이슈 ID 목록을 먼저 조회 (페이징 적용)
            OrderSpecifier<?> orderSpecifier = getSortOrder(filter.sort(), filter.order());
            ids = queryFactory
                    .select(qIssue.id)
                    .from(qIssue)
                    .where(builder)
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .orderBy(orderSpecifier)
                    .fetch();
        }

        if (ids.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 2. 상세 정보들을 fetch join으로 함께 가져옴
        List<Issue> issues = issueRepository.findAllByIdInWithDetails(ids);

        // 정렬 유지를 위해 ID 목록 순서대로 다시 정렬
        List<Long> finalIds = ids;
        List<Issue> sortedIssues = issues.stream()
                .sorted((i1, i2) -> Integer.compare(finalIds.indexOf(i1.getId()), finalIds.indexOf(i2.getId())))
                .toList();

        // 3. 전체 카운트 조회
        long total = queryFactory
                .select(qIssue.id)
                .from(qIssue)
                .where(builder)
                .fetchCount();

        List<IssueResponse> dtoList = sortedIssues.stream().map(this::toResponse).toList();
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
        // 담당자 ID로 필터링
        if (filter.assigneeIds() != null && !filter.assigneeIds().isEmpty()) {
            builder.and(qIssue.issueAssignees.any().user.id.in(filter.assigneeIds()));
        }
        // 라벨 ID로 필터링
        if (filter.labelIds() != null && !filter.labelIds().isEmpty()) {
            builder.and(qIssue.issueLabels.any().label.id.in(filter.labelIds()));
        }
        // 텍스트로 검색 (제목, 작성자, 담당자, 라벨 이름)
        if (filter.query() != null && !filter.query().isBlank()) {
            String searchQuery = filter.query();
            BooleanBuilder queryBuilder = new BooleanBuilder();
            queryBuilder.or(qIssue.title.containsIgnoreCase(searchQuery));
            queryBuilder.or(qIssue.author.username.containsIgnoreCase(searchQuery));
            // 담당자와 라벨 검색은 조인이 필요하므로 별도 쿼리가 더 효율적일 수 있으나, 여기서는 간소화된 형태로 유지
             queryBuilder.or(qIssue.issueAssignees.any().user.username.containsIgnoreCase(searchQuery));
             queryBuilder.or(qIssue.issueLabels.any().label.name.containsIgnoreCase(searchQuery));
            builder.and(queryBuilder);
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
    @Cacheable(value = "issue", key = "#id")
    public IssueResponse getIssue(Long id) {
        Issue issue = issueRepository.findWithDetailsById(id)
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
    @Caching(
            put = { @CachePut(value = "issue", key = "#id") }, // 'issue' 캐시는 최신 내용으로 업데이트합니다.
            evict = {
                    @CacheEvict(value = "issues", allEntries = true),
                    @CacheEvict(value = "issue_navigation", allEntries = true)
            } // 'issues'와 'issue_navigation' 목록 캐시는 그냥 비웁니다.
    )
    public IssueResponse updateIssue(Long id, UpdateIssueRequest request) {
        Issue issue = issueRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found"));

        if (request.title() != null) issue.setTitle(request.title());
        if (request.description() != null) issue.setDescription(request.description());

        // 다중 담당자 매핑 업데이트
        if (request.assigneeIds() != null) {
            issueAssigneeRepository.deleteByIssueId(issue.getId());
            List<User> newAssignees = userRepository.findAllById(request.assigneeIds());
            Set<IssueAssignee> newIssueAssignees = newAssignees.stream()
                    .map(assignee -> new IssueAssignee(issue, assignee))
                    .collect(Collectors.toSet());
            issueAssigneeRepository.saveAll(newIssueAssignees);
            issue.setIssueAssignees(newIssueAssignees);
        }

        // 다중 라벨 매핑 업데이트
        if (request.labelIds() != null) {
            issueLabelRepository.deleteByIssueId(issue.getId());                        // 1. 기존 연결 삭제
            List<Label> newLabels = labelRepository.findAllById(request.labelIds());    // 2. 찾기
            Set<IssueLabel> newIssueLabels = newLabels.stream()
                    .map(label -> new IssueLabel(issue, label))
                    .collect(Collectors.toSet());                                      // 3. 새 연결 생성
            issueLabelRepository.saveAll(newIssueLabels);                               // 4. DB 저장
            issue.setIssueLabels(newIssueLabels);                                       // 5. 메모리 객체 동기화
        }

        return toResponse(issue);
    }

    /**
     * 이슈 상태 변경
     * @param id 상태 변경할 Issue ID
     * @param status 변경할 상태 (OPEN, IN_PROGRESS, CLOSED)
     * @return 상태가 변경된 IssueResponse
     */
    @Caching(
            put = { @CachePut(value = "issue", key = "#id") },
            evict = {
                    @CacheEvict(value = "issues", allEntries = true),
                    @CacheEvict(value = "issue_navigation", allEntries = true)
            }
    )
    public IssueResponse changeIssueStatus(Long id, String status) {
        Issue issue = issueRepository.findWithDetailsById(id)
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
    @Caching(
            evict = {
                    @CacheEvict(value = "issue", key = "#id"),
                    @CacheEvict(value = "issues", allEntries = true),
                    @CacheEvict(value = "issue_navigation", allEntries = true)
            }
    )
    public void deleteIssue(Long id) {
        if (!issueRepository.existsById(id)) {
            throw new NotFoundException("Issue not found");
        }
        issueRepository.deleteById(id);
    }

    /**
     * 특정 필터 조건 하에서, 현재 이슈의 이전/다음 이슈 ID 조회
     * @param currentIssueId 현재 보고 있는 이슈의 ID
     * @param filter 목록 페이지에서 사용된 필터 및 정렬 조건
     * @return 이전/다음 이슈 ID를 담은 DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "issue_navigation", key = "#currentIssueId + '-' + #filter.toString()")
    public IssueNavigationResponse getIssueNavigation(Long currentIssueId, IssueFilterRequest filter) {
        // 1. 목록 조회와 동일한 필터, 정렬 조건을 가져옵니다.
        BooleanBuilder builder = createFilterBuilder(filter);
        OrderSpecifier<?> orderSpecifier = getSortOrder(filter.sort(), filter.order());

        // 2. 페이징 없이, 필터링되고 정렬된 전체 이슈 ID 목록을 조회합니다.
        List<Long> allFilteredIds = queryFactory
                .select(qIssue.id)
                .from(qIssue)
                .where(builder)
                .orderBy(orderSpecifier)
                .fetch();

        if (allFilteredIds.isEmpty()) {
            return new IssueNavigationResponse(null, null);
        }

        // 3. 전체 목록에서 현재 이슈 ID의 인덱스(순서)를 찾습니다.
        int currentIndex = allFilteredIds.indexOf(currentIssueId);

        if (currentIndex == -1) {
            // 현재 이슈가 필터 조건에 맞지 않는 경우 (예: 상태가 바뀜)
            return new IssueNavigationResponse(null, null);
        }

        // 4. 인덱스를 기준으로 이전(-1)과 다음(+1) ID를 결정합니다.
        Long previousId = (currentIndex > 0) ? allFilteredIds.get(currentIndex - 1) : null;
        Long nextId = (currentIndex < allFilteredIds.size() - 1) ? allFilteredIds.get(currentIndex + 1) : null;

        return new IssueNavigationResponse(previousId, nextId);
    }

    private IssueResponse toResponse(Issue issue) {
        List<Long> assigneeIds = issue.getIssueAssignees().stream()
                .map(issueAssignee -> issueAssignee.getUser().getId())
                .toList();

        List<Long> labelIds = issue.getIssueLabels().stream()
                .map(issueLabel -> issueLabel.getLabel().getId())
                .toList();

        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus().name(),
                issue.getAuthor().getId(),
                assigneeIds,
                labelIds,
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}
