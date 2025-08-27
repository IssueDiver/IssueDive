package com.issueDive.dto;

public record UpdateIssueRequest(
        String title,
        String description,
        Long assigneeId
) {}

