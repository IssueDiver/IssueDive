package com.issueDive.controller;

import com.issueDive.dto.ApiCommonResponse;
import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.LabelResponse;
import com.issueDive.dto.UpdateLabelRequest;
import com.issueDive.dto.IssueLabelsResponse;
import com.issueDive.service.LabelService;
import com.issueDive.service.IssueLabelService;
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
import java.util.Map;

@Tag(name = "Label", description = "레이블 관리 API")
@RestController
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;
    private final IssueLabelService issueLabelService;

    @Operation(summary = "레이블 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "레이블 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 값", content = @Content)
    })
    @PostMapping("/labels")
    public ResponseEntity<ApiCommonResponse<LabelResponse>> createLabel(
            @Valid @RequestBody CreateLabelRequest request){

        LabelResponse data = labelService.createLabel(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiCommonResponse.ok(data));

    }

    @Operation(summary = "전체 레이블 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/labels")
    public ResponseEntity<ApiCommonResponse<List<LabelResponse>>> getLabels(){
        List<LabelResponse> data = labelService.getLabels();

        return ResponseEntity.ok(ApiCommonResponse.ok(data));
    }

    @Operation(summary = "단일 레이블 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 레이블", content = @Content)
    })
    @GetMapping("/labels/{labelId}")
    public ResponseEntity<ApiCommonResponse<LabelResponse>> getLabelById(
            @Parameter(description = "레이블 ID", required = true) @PathVariable Long labelId){
        LabelResponse data = labelService.getLabel(labelId);

        return ResponseEntity.ok(ApiCommonResponse.ok(data));
    }

    @Operation(summary = "레이블 정보 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 레이블", content = @Content)
    })
    @PatchMapping("/labels/{labelId}")
    public ResponseEntity<ApiCommonResponse<LabelResponse>> updateLabel(
            @Parameter(description = "레이블 ID", required = true) @PathVariable Long labelId,
            @Valid @RequestBody UpdateLabelRequest request){
        LabelResponse data = labelService.updateLabel(labelId, request);

        return ResponseEntity.ok(ApiCommonResponse.ok(data));
    }

    @Operation(summary = "레이블 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 레이블", content = @Content)
    })
    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<ApiCommonResponse<Map<String, String>>> deleteLabel(
            @Parameter(description = "레이블 ID", required = true) @PathVariable Long labelId){
        labelService.deleteLabel(labelId);
        return ResponseEntity.ok(ApiCommonResponse.ok(Map.of("message", "Label " + labelId + " deleted successfully")));
    }

    @Operation(summary = "이슈에 레이블 추가", description = "특정 이슈에 하나 이상의 레이블을 연결합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "레이블 추가 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈 또는 레이블", content = @Content)
    })
    @PostMapping("/issues/{issueId}/labels")
    public ResponseEntity<ApiCommonResponse<IssueLabelsResponse>> addLabels(
            @Parameter(description = "이슈 ID", required = true) @PathVariable Long issueId,
            @RequestBody List<Long> labelIds) {
        IssueLabelsResponse data = issueLabelService.addLabelsToIssue(issueId, labelIds);
        return ResponseEntity.ok(ApiCommonResponse.ok(data));
    }

    @Operation(summary = "이슈에서 레이블 제거", description = "특정 이슈와 특정 레이블의 연결을 끊습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "레이블 제거 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이슈 또는 레이블 관계", content = @Content)
    })
    @DeleteMapping("/issues/{issueId}/labels/{labelId}")
    public ResponseEntity<ApiCommonResponse<IssueLabelsResponse>> deleteLabelFromIssue(
            @Parameter(description = "이슈 ID", required = true) @PathVariable Long issueId,
            @Parameter(description = "제거할 레이블 ID", required = true) @PathVariable Long labelId) {
        IssueLabelsResponse data = issueLabelService.deleteLabelFromIssue(issueId, labelId);
        return ResponseEntity.ok(ApiCommonResponse.ok(data));
    }
}
