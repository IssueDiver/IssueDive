package com.issueDive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CountCommentResponse {
    private Long issueId;
    private Long count;
}
