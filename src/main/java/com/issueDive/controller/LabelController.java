package com.issueDive.controller;

import com.issueDive.dto.ApiResponse;
import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.LabelResponse;
import com.issueDive.service.LabelService;
import com.issueDive.util.ServiceResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LabelController {
    private final LabelService _labelService;

    @PostMapping("/labels")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            @Valid @RequestBody CreateLabelRequest request){

        ServiceResult<LabelResponse> _result = _labelService.createLabel(request);

        if(_result.isSuccess()){
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(_result.getData()));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(_result.getCode(), _result.getMessage()));
    }

    @GetMapping("/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getLabels(){
        ServiceResult<List<LabelResponse>> _result = _labelService.getLabels();

        //목록 조회는 실패 케이스가 없으므로 ok 고정
        return ResponseEntity.ok(ApiResponse.success(_result.getData()));
    }

    @GetMapping("/labels/{id}")
    public ResponseEntity<ApiResponse<LabelResponse>> getLabelById(@PathVariable Long id){
        ServiceResult<LabelResponse> _result = _labelService.getLabel(id);
        if(_result.isSuccess()){
            return ResponseEntity.ok(ApiResponse.success(_result.getData()));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(_result.getCode(), _result.getMessage()));
    }
}
