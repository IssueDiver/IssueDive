package com.example.issueDive.controller;


import com.example.issueDive.dto.ApiResponse;
import com.example.issueDive.dto.CreateLabelRequest;
import com.example.issueDive.dto.LabelResponse;
import com.example.issueDive.service.LabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;

    @PostMapping("/labels")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            @Valid @RequestBody CreateLabelRequest request){

        LabelResponse data = labelService.createLabel(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data));

    }

    @GetMapping("/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getLabels(){
        List<LabelResponse> data = labelService.getLabels();

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/labels/{id}")
    public ResponseEntity<ApiResponse<LabelResponse>> getLabelById(@PathVariable Long id){
        LabelResponse data = labelService.getLabel(id);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
