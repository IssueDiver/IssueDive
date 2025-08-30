package com.issueDive.controller;

import com.issueDive.dto.*;
import com.issueDive.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/issues/{issueId}/comments")
public class CommentController {
    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComment(@PathVariable Long issueId){
        List<CommentResponse> tree = commentService.getTreeByIssue(issueId);
        return ResponseEntity.ok(ApiResponse.ok(tree));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(@PathVariable Long issueId, @RequestBody @Valid CreateCommentRequest request
                                                        ,@RequestHeader("X-USER-ID") Long userId){

        CommentResponse created = commentService.createComment(issueId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(@PathVariable Long issueId, @PathVariable Long commentId, @RequestBody @Valid UpdateCommentRequest request, @RequestHeader("X-USER-ID") Long userId){
        CommentResponse updated = commentService.updateComment(issueId, commentId, request, userId);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long issueId, @PathVariable Long commentId, @RequestHeader("X-USER-ID") Long userId){
        commentService.deleteComment(issueId, commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public  ResponseEntity<ApiResponse<CountCommentResponse>> countComment(@PathVariable Long issueId){
        CountCommentResponse response = commentService.countByIssue(issueId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}