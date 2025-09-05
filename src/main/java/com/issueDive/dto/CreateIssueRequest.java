package com.issueDive.dto;

import java.util.List;

public record CreateIssueRequest(
        String title,
        String description,
        List<Long> assigneeIds,
        List<Long> labels
) {
}
