package com.issueDive.dto;

public record CreateIssueRequest(
        String title,
        String description,
        Long assigneeId
) {
}
