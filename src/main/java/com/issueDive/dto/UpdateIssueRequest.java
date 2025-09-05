package com.issueDive.dto;

import java.util.List;

public record UpdateIssueRequest(
        String title,
        String description,
        List<Long> assigneeIds,
        List<Long> labelIds
) {}

