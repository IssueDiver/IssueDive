package com.issueDive.controller;

import com.issueDive.dto.*;
import com.issueDive.entity.User;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.repository.UserRepository;
import com.issueDive.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.issueDive.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Issue", description = "이슈 관리 API")
@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final UserService userService;

    /**
     * Create
     * @param request title, description, assignee(uid)
     * @return 공통 응답 포맷 + 생성된 이슈 dto
     */
    @Operation(summary = "이슈 생성", description = "새로운 이슈를 생성합니다. 요청 본문에 제목, 설명 등을 포함해야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이슈 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 값", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ApiCommonResponse<IssueResponse>> createIssue(
            @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDTO user = userService.findUserByEmail(userDetails.getUsername());
        IssueResponse issue = issueService.createIssue(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiCommonResponse.ok(issue));
    }

    /**
     * Read: 다중 조회(필터링, 페이징)
     * @param filter status, authorId, labelIds, page, size, sort, order
     * @return 공통 응답 포맷 + 조회한 이슈 dto 리스트(페이지)
     */
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
                        filter.assigneeIds(),
                        filter.labelIds(),
                        filter.page() != null ? filter.page() : 0,
                        filter.size() != null ? filter.size() : 10,
                        filter.sort() != null ? filter.sort() : "createdAt",
                        filter.order() != null ? filter.order() : "desc",
                        filter.query()
                ));
        return ResponseEntity.status(HttpStatus.OK).body(ApiCommonResponse.ok(issue));
    }

    /**
     * Read: 단건 조회
     * @param id 조회할 이슈
     * @return 공통 응답 포맷 + 해당 이슈 dto
     */
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

    /**
     * Update (PATCH /issues/{id})
     * @param id 수정할 이슈 id
     * @param request title, description, assignee(uid), labelIds
     * @return 공통 응답 포맷 + 수정된 이슈 dto
     */
    @Operation(summary = "이슈 정보 수정", description = "특정 이슈의 제목, 설명 등 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiCommonResponse<IssueResponse>> patchIssue(
            @Parameter(description = "수정할 이슈의 ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody UpdateIssueRequest request) {
        return ResponseEntity.ok(ApiCommonResponse.ok(issueService.updateIssue(id, request)));
    }

    /**
     * Update (PUT /issues/{id})
     * @param id 수정할 이슈 id
     * @param request title, description, assignee(uid), labelIds
     * @return 공통 응답 포맷 + 수정된 이슈 dto
     */
    @Operation(summary = "이슈 정보 전체 수정", description = "특정 이슈의 전체 정보(제목, 설명 등)를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiCommonResponse<IssueResponse>> putIssue(
            @Parameter(description = "수정할 이슈의 ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody UpdateIssueRequest request) {
        return ResponseEntity.ok(ApiCommonResponse.ok(issueService.updateIssue(id, request)));
    }

    /**
     * 이슈 상태 변경 PATCH /issues/{id}/status
     * @param id 이슈 ID
     * @param body { "status": "OPEN" or "CLOSED" }
     * @return 변경된 상태 IssueResponse 반환
     */
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

    /**
     * Delete
     * @param id 삭제할 이슈
     * @return 공통 응답 포맷 + 성공 메세지
     */
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
