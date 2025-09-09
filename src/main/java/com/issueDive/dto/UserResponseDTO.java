package com.issueDive.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class UserResponseDTO implements Serializable {
    private Long id;
    private String username;
    private String email;
}
