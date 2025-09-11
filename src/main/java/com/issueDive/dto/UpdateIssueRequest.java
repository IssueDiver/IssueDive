package com.issueDive.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateIssueRequest(

        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        String title,

        @Size(max = 65535, message = "설명은 65535자를 초과할 수 없습니다.")
        String description,

        List<Long> assigneeIds,
        List<Long> labelIds

) {}

