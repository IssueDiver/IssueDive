package com.issueDive.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {
    @NotBlank
    @Size(max = 65535, message = "댓글 내용은 65535자를 초과할 수 없습니다.")
    private String description;
    private Long parentId;
}
