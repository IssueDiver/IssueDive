package com.example.issueDive.controller;

import com.example.issueDive.dto.CommentResponse;
import com.example.issueDive.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/issues/{issues_id}/comments")
public class CommentController {
    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComment(@PathVariable Long issueId){

    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(){

    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(){

    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(){

    }

    @GetMapping("/count")
    public  ResponseEntity<CommentCountResponse> countComment(){

    }
}
//package com.BugJava.EduConnect.assignment.controller;
//import com.BugJava.EduConnect.assignment.dto.AssignmentCommentRequest;
//import com.BugJava.EduConnect.assignment.dto.AssignmentCommentResponse;
//import com.BugJava.EduConnect.assignment.service.AssignmentCommentService;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//        import java.net.URI;
//
//@RestController
//@RequestMapping("/api/assignments/{assignmentId}/comments")
//@RequiredArgsConstructor
//public class AssignmentCommentController {
//
//    private final AssignmentCommentService assignmentCommentService;
//
//    //댓글 생성
//    @PostMapping
//    public ResponseEntity<?> createComment(@PathVariable Long assignmentId, @Valid @RequestBody AssignmentCommentRequest request,
//                                           @AuthenticationPrincipal Long userId){
//
//        AssignmentCommentResponse created = assignmentCommentService.createComment(assignmentId, request, userId);
//
//        URI location = URI.create("/api/assignments/" + assignmentId + "/comments/" + created.getId());
//        return ResponseEntity.created(location).body(created);
//    }
//
//    //댓글 수정
//    @PatchMapping("/{id}")
//    public ResponseEntity<AssignmentCommentResponse> updateComment(@PathVariable Long id, @RequestBody @Valid AssignmentCommentRequest request,
//                                                                   @AuthenticationPrincipal Long userId) {
//        return ResponseEntity.ok(assignmentCommentService.updateComment(id, request, userId));
//    }
//
//    //댓글 삭제
//    @DeleteMapping("/{id}")
//    public ResponseEntity<AssignmentCommentResponse> deleteComment(@PathVariable Long id, @AuthenticationPrincipal Long userId){
//        assignmentCommentService.deleteComment(id, userId);
//        return ResponseEntity.noContent().build();
//    }
//}