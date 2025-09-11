package com.issueDive.dto;

import com.issueDive.entity.Label;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@NoArgsConstructor
public class LabelResponse implements Serializable {
    private Long id;
    private String name;
    private String color;
    private String description;
    private Long issueOpenCount;

    public LabelResponse(Long id, String name, String color, String description, Long issueOpenCount) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.description = description;
        this.issueOpenCount = issueOpenCount != null ? issueOpenCount : 0L; // COUNT 결과가 NULL일 경우 0으로 처리
    }

    public static LabelResponse from(Label label, long issueOpenCount) {
        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .description(label.getDescription())
                .issueOpenCount(issueOpenCount)
                .build();
    }
}
