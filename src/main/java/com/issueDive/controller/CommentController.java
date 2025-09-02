package com.issueDive.controller;

import com.issueDive.dto.*;
import com.issueDive.entity.User;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.repository.UserRepository;
import com.issueDive.service.CommentService;
import com.issueDive.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/issues/{issueId}/comments")
public class CommentController {
    private final CommentService commentService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComment(@PathVariable Long issueId){
        List<CommentResponse> tree = commentService.getTreeByIssue(issueId);
        return ResponseEntity.ok(ApiResponse.ok(tree));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long issueId,
            @RequestBody @Valid CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails){
        UserResponseDTO user = userService.findUserByEmail(userDetails.getUsername());
        CommentResponse created = commentService.createComment(issueId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long issueId,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails){
        UserResponseDTO user = userService.findUserByEmail(userDetails.getUsername());
        CommentResponse updated = commentService.updateComment(issueId, commentId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long issueId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails){
        UserResponseDTO user = userService.findUserByEmail(userDetails.getUsername());
        commentService.deleteComment(issueId, commentId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public  ResponseEntity<ApiResponse<CountCommentResponse>> countComment(@PathVariable Long issueId){
        CountCommentResponse response = commentService.countByIssue(issueId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}