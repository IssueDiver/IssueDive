package com.issueDive.dto;

import com.issueDive.entity.Label;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@Builder
public class LabelResponse implements Serializable {
    private Long id;
    private String name;
    private String color;
    private String description;
    private Long issueOpenCount;

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
