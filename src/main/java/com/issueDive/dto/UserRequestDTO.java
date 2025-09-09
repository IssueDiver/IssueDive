package com.issueDive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
public class UserRequestDTO {
    @NotBlank(message = "사용자명은 필수입니다.")
    @Size(min = 1, max = 50, message = "사용자명은 1자 이상 50자 이하로 입력해주세요.")
    private String username;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
    private String password;
}
