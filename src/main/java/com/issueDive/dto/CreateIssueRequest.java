package com.issueDive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateIssueRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        String title,

        @Size(max = 65535, message = "설명은 65535자를 초과할 수 없습니다.")
        String description,

        List<Long> assigneeIds,
        List<Long> labels
) {
}
