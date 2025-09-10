package com.issueDive.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponseDTO user;

    // 리프레시 토큰을 포함한 팩토리 메서드로 수정
    public static JwtResponse of (String accessToken, String refreshToken, String tokenType, Long expiresIn, UserResponseDTO user){
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken) // 리프레시 토큰 추가
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }

    // 이전 버전과의 호환성을 위한 오버로드 메서드 (테스트용)
    public static JwtResponse of (String accessToken, String tokenType, Long expiresIn, UserResponseDTO user){
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(null)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
