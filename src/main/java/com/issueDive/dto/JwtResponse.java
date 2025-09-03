package com.issueDive.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class JwtResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponseDTO user;

    public static JwtResponse of (String accessToken, String tokenType, Long expiresIn, UserResponseDTO user){
        return JwtResponse.builder()
                .accessToken(accessToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
