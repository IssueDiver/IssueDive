package com.issueDive.controller;

import com.issueDive.dto.ApiResponse;
import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.LabelResponse;
import com.issueDive.dto.UpdateLabelRequest;
import com.issueDive.service.LabelService;
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
    @GetMapping("/labels/{id}")
    public ResponseEntity<ApiResponse<LabelResponse>> getLabelById(@PathVariable Long id){
        LabelResponse data = labelService.getLabel(id);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    //라벨 수정
    @PatchMapping("/labels/{id}")
    public ResponseEntity<ApiResponse<LabelResponse>> updateLabel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLabelRequest request){
        LabelResponse data = labelService.updateLabel(id, request);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    //라벨 삭제
    @DeleteMapping("/labels/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteLabel(@PathVariable Long id){
        labelService.deleteLabel(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Label " + id + " deleted successfully")));
    }
}
