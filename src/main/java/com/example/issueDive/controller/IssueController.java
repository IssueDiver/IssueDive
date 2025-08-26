package com.example.issueDive.controller;

import com.example.issueDive.dto.CreateIssueRequest;
import com.example.issueDive.dto.IssueResponse;
import com.example.issueDive.dto.UpdateIssueRequest;
import com.example.issueDive.service.IssueService;
import com.example.issueDive.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    /**
     * Create
     * @param request title, description, assignee(uid)
     * @return 공통 응답 포맷 + 생성된 이슈 dto
     */
    @PostMapping
    public ResponseEntity<ApiResponse<IssueResponse>> createIssue(@RequestBody CreateIssueRequest request) {
        Long currentUserId = 1L; // 임시, TODO: 로그인 세션/토큰 붙이면 교체
        IssueResponse issue = issueService.createIssue(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(issue));
    }

    /**
     * Read
     * @param id 조회할 이슈
     * @return 공통 응답 포맷 + 해당 이슈 dto
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IssueResponse>> getIssue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.getIssue(id)));
    }

    /**
     * Update
     * @param id 수정할 이슈 id
     * @param request title, description, assignee(uid)
     * @return 공통 응답 포맷 + 수정된 이슈 dto
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IssueResponse>> updateIssue(@PathVariable Long id,
                                                                  @RequestBody UpdateIssueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.updateIssue(id, request)));
    }

    /**
     * 이슈 상태 변경 PATCH /issues/{id}/status
     * @param id 이슈 ID
     * @param body { "status": "OPEN" or "CLOSED" }
     * @return 변경된 상태 IssueResponse 반환
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<IssueResponse>> changeIssueStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        IssueResponse response = issueService.changeIssueStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Delete
     * @param id 삭제할 이슈
     * @return 공통 응답 포맷 + 성공 메세지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Issue " + id + " deleted successfully")));
    }
}
