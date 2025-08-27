package com.example.issueDive.dto;

import java.time.LocalDateTime;
import java.util.List;

public record IssueResponse(
        Long id,
        String title,
        String description,
        String status,
        Long authorId,
        Long assigneeId,
        List<Long> labelIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
