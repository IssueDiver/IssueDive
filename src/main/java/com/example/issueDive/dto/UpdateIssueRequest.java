package com.example.issueDive.dto;

public record UpdateIssueRequest(
        String title,
        String description,
        Long assigneeId
) {}

