package com.example.issueDive.dto;

import java.time.LocalDateTime;

public record IssueResponse(
        Long id,
        String title,
        String description,
        String status,
        Long authorId,
        Long assigneeId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
