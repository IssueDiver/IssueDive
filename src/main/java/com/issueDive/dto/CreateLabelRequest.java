package com.issueDive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabelRequest {

    @NotBlank(message = "라벨명은 필수입니다.")
    @Size(max = 50, message = "라벨명은 50자를 초과할 수 없습니다.")
    private String name;

    @NotBlank(message = "색상 코드는 필수입니다.")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상 코드는 #RRGGBB 형식이어야 합니다.")
    private String color;

    @Size(max = 200, message = "설명은 200자를 초과할 수 없습니다.")
    private String description;
}
