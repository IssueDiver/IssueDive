package com.issueDive.controller;

import com.issueDive.dto.*;
import com.issueDive.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Issue", description = "이슈 관리 API")
@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @Operation(summary = "이슈 생성", description = "새로운 이슈를 생성합니다. 요청 본문에 제목, 설명 등을 포함해야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이슈 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 값", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ApiCommonResponse<IssueResponse>> createIssue(@RequestBody CreateIssueRequest request) {
        Long currentUserId = 1L; // 임시, TODO: 로그인 세션/토큰 붙이면 교체
        IssueResponse issue = issueService.createIssue(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiCommonResponse.ok(issue));
    }

    @Operation(summary = "이슈 목록 필터링 조회", description = "다양한 조건(상태, 작성자, 담당자, 레이블 등)으로 이슈를 필터링하고 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이슈 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiCommonResponse<Page<IssueResponse>>> getIssues(
            @Parameter(description = "이슈 상태, 작성자 ID, 담당자 ID, 레이블 ID 목록, 페이징 정보 등을 담는 필터 객체")
            @Valid IssueFilterRequest filter) {
        Page<IssueResponse> issue = issueService.getFilteredIssues(
                new IssueFilterRequest(
                        filter.status(),
                        filter.authorId(),
                        filter.assigneeId(),
                        filter.labelIds(),
                        filter.page() != null ? filter.page() : 0,
                        filter.size() != null ? filter.size() : 10,
                        filter.sort() != null ? filter.sort() : "createdAt",
                        filter.order() != null ? filter.order() : "desc"
                ));
        return ResponseEntity.status(HttpStatus.OK).body(ApiCommonResponse.ok(issue));
    }

    @Operation(summary = "이슈 단건 조회", description = "ID를 이용하여 특정 이슈의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiCommonResponse<IssueResponse>> getIssue(
            @Parameter(description = "조회할 이슈의 ID", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiCommonResponse.ok(issueService.getIssue(id)));
    }

    @Operation(summary = "이슈 정보 수정", description = "특정 이슈의 제목, 설명 등 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiCommonResponse<IssueResponse>> updateIssue(
            @Parameter(description = "수정할 이슈의 ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody UpdateIssueRequest request) {
        return ResponseEntity.ok(ApiCommonResponse.ok(issueService.updateIssue(id, request)));
    }

    @Operation(summary = "이슈 상태 변경", description = "이슈의 상태를 'OPEN' 또는 'CLOSED'로 변경합니다. 요청 본문 예시: { \"status\": \"CLOSED\" }")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상태 값 (OPEN, CLOSED만 가능)", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiCommonResponse<IssueResponse>> changeIssueStatus(
            @Parameter(description = "상태를 변경할 이슈의 ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        IssueResponse response = issueService.changeIssueStatus(id, status);
        return ResponseEntity.ok(ApiCommonResponse.ok(response));
    }

    @Operation(summary = "이슈 삭제", description = "ID를 이용하여 특정 이슈를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiCommonResponse<Map<String, String>>> deleteIssue(
            @Parameter(description = "삭제할 이슈의 ID", required = true, example = "1")
            @PathVariable Long id) {
        issueService.deleteIssue(id);
        return ResponseEntity.ok(ApiCommonResponse.ok(Map.of("message", "Issue " + id + " deleted successfully")));
    }
}
