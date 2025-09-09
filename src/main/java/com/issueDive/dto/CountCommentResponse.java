package com.issueDive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class CountCommentResponse implements Serializable {
    private Long issueId;
    private Long count;
}
