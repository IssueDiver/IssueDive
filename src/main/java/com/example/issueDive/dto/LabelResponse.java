package com.example.issueDive.dto;

import com.example.issueDive.entity.Label;
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
                .created_at(label.getCreated_at())
                .updated_at(label.getUpdated_at())
                .build();
    }
}
