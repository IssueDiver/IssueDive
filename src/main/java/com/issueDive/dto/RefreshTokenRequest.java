package com.issueDive.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RefreshTokenRequest {
    private String refreshToken;
}
//추가