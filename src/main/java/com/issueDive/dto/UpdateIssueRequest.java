package com.issueDive.dto;

import java.util.List;

public record UpdateIssueRequest(
        String title,
        String description,
        Long assigneeId,
        List<Long> labelIds
) {}

