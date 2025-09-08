package com.issueDive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record IssueFilterRequest(
        @Pattern(regexp = "open|closed|in_progress|OPEN|CLOSED|IN_PROGRESS", message = "상태 값은 open, closed, in_progress 중 하나여야 합니다.") String status,
        Long authorId,
        List<Long> assigneeIds,
        List<Long> labelIds,
        @Min(0) Integer page,
        @Min(1) Integer size,
        @Pattern(regexp = "createdAt|updatedAt") String sort,
        @Pattern(regexp = "asc|desc|ASC|DESC") String order,
        String query
) {}