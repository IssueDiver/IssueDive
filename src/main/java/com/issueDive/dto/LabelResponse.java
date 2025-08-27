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

    public static LabelResponse from(Label _label) {
        return LabelResponse.builder()
                .id(_label.getId())
                .name(_label.getName())
                .color(_label.getColor())
                .description(_label.getDescription())
                .created_at(_label.getCreated_at())
                .updated_at(_label.getUpdated_at())
                .build();
    }
}
