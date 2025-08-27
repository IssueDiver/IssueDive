package com.example.issueDive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record IssueFilterRequest(
        @Pattern(regexp = "open|closed|OPEN|CLOSED") String status,
        Long authorId,
        Long assigneeId,
        List<Long> labelIds,
        @Min(0) Integer page,
        @Min(1) Integer size,
        @Pattern(regexp = "createdAt|updatedAt") String sort,
        @Pattern(regexp = "asc|desc|ASC|DESC") String order
) {}