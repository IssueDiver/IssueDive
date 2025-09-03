package com.issueDive.dto;

import java.util.List;

public record CreateIssueRequest(
        String title,
        String description,
        Long assigneeId,
        List<Long> labels
) {
}
