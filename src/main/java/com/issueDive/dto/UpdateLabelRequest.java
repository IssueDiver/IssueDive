package com.issueDive.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLabelRequest {

    @Size(max = 50, message = "라벨 이름은 50자를 초과할 수 없습니다.")
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상 코드는 #RRGGBB 형식이어야 합니다.")
    private String color;

    @Size(max = 200, message = "설명은 200자를 초과할 수 없습니다.")
    private String description;

}
