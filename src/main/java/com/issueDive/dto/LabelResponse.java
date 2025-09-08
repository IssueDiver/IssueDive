package com.issueDive.dto;

import com.issueDive.entity.Label;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class LabelResponse {
    private Long id;
    private String name;
    private String color;
    private String description;
    private Long openCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LabelResponse from(Label label, long openCount) {

        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .description(label.getDescription())
                .openCount(openCount)
                .createdAt(label.getCreatedAt())
                .updatedAt(label.getUpdatedAt())
                .build();
    }
}
