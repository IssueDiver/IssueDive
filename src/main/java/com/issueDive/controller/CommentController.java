package com.issueDive.controller;

import com.issueDive.dto.*;
import com.issueDive.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment", description = "댓글 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/issues/{issueId}/comments")
public class CommentController {
    private final CommentService commentService;

    @Operation(summary = "특정 이슈의 댓글 목록 조회", description = "특정 이슈에 달린 모든 댓글을 계층 구조(트리)로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @GetMapping
    public ResponseEntity<ApiCommonResponse<List<CommentResponse>>> getComment(
            @Parameter(description = "이슈 ID", required = true) @PathVariable Long issueId){
        List<CommentResponse> tree = commentService.getTreeByIssue(issueId);
        return ResponseEntity.ok(ApiCommonResponse.ok(tree));
    }

    @Operation(summary = "댓글 생성", description = "특정 이슈에 새로운 댓글을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "댓글 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 값", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ApiCommonResponse<CommentResponse>> createComment(
            @Parameter(description = "이슈 ID", required = true) @PathVariable Long issueId,
            @RequestBody @Valid CreateCommentRequest request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId){

        CommentResponse created = commentService.createComment(issueId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiCommonResponse.ok(created));
    }

    @Operation(summary = "댓글 수정", description = "특정 댓글의 내용을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈 또는 댓글", content = @Content)
    })
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiCommonResponse<CommentResponse>> updateComment(
            @Parameter(description = "이슈 ID", required = true) @PathVariable Long issueId,
            @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId){
        CommentResponse updated = commentService.updateComment(issueId, commentId, request, userId);
        return ResponseEntity.ok(ApiCommonResponse.ok(updated));
    }

    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈 또는 댓글", content = @Content)
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @Parameter(description = "이슈 ID", required = true) @PathVariable Long issueId,
            @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId){
        commentService.deleteComment(issueId, commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 이슈의 댓글 수 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈", content = @Content)
    })
    @GetMapping("/count")
    public  ResponseEntity<ApiCommonResponse<CountCommentResponse>> countComment(
            @Parameter(description = "이슈 ID", required = true) @PathVariable Long issueId){
        CountCommentResponse response = commentService.countByIssue(issueId);
        return ResponseEntity.ok(ApiCommonResponse.ok(response));
    }
}