package com.example.issueDive.service;

import com.example.issueDive.dto.CreateIssueRequest;
import com.example.issueDive.dto.IssueResponse;
import com.example.issueDive.dto.UpdateIssueRequest;
import com.example.issueDive.entity.Issue;
import com.example.issueDive.entity.IssueStatus;
import com.example.issueDive.entity.User;
import com.example.issueDive.exception.NotFoundException;
import com.example.issueDive.repository.IssueRepository;
import com.example.issueDive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueService {

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
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setAuthor(author);
        issue.setAssignee(assignee);
        issue.setStatus(IssueStatus.OPEN);

        Issue saved = issueRepository.save(issue);
        return toResponse(saved);
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
        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus().name(),
                issue.getAuthor().getId(),
                issue.getAssignee() != null ? issue.getAssignee().getId() : null,
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}
