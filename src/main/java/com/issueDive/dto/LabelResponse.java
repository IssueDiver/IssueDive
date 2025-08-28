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
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    public static LabelResponse from(Label label) {

        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .description(label.getDescription())
                .created_at(label.getCreatedAt())
                .updated_at(label.getUpdatedAt())
                .build();
    }
}
