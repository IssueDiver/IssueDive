package com.issueDive.dto;

import com.issueDive.config.NoXss;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class LoginRequestDTO {

    @Email(message = "올바른 email이 아닙니다.")
    @NotBlank(message = "email은 필수입니다.")
    @NoXss
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @NoXss
    private String password;
}
