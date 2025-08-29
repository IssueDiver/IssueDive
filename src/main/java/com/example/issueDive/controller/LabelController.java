package com.example.issueDive.controller;


import com.example.issueDive.dto.*;
import com.example.issueDive.service.IssueLabelService;
import com.example.issueDive.service.LabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;
    private final IssueLabelService issueLabelService;

    //라벨 생성
    @PostMapping("/labels")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            @Valid @RequestBody CreateLabelRequest request){

        LabelResponse data = labelService.createLabel(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data));

    }

    //라벨 목록 조회
    @GetMapping("/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getLabels(){
        List<LabelResponse> data = labelService.getLabels();

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    //단일 라벨 조회
    @GetMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<LabelResponse>> getLabelById(@PathVariable Long labelId){
        LabelResponse data = labelService.getLabel(labelId);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    //라벨 수정
    @PatchMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<LabelResponse>> updateLabel(
            @PathVariable Long labelId,
            @Valid @RequestBody UpdateLabelRequest request){
        LabelResponse data = labelService.updateLabel(labelId, request);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    //라벨 삭제
    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteLabel(@PathVariable Long labelId){
        labelService.deleteLabel(labelId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Label " + labelId + " deleted successfully")));
    }

    @PostMapping("/issues/{issueId}/labels")
    public ResponseEntity<ApiResponse<IssueLabelsResponse>> addLabels(
            @PathVariable Long issueId,
            @RequestBody List<Long> labelIds) {
        IssueLabelsResponse data = issueLabelService.addLabelsToIssue(issueId, labelIds);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/issues/{issueId}/labels/{labelId}")
    public ResponseEntity<ApiResponse<LabelResponse>> deleteLabelFromIssue(
            @PathVariable Long issueId,
            @PathVariable Long labelId) {
        LabelResponse data = issueLabelService.deleteLabelFromIssue(issueId, labelId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
