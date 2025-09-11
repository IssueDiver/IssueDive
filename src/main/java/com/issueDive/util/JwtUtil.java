package com.issueDive.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springdoc.core.configuration.SpringDocGroovyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    //추후에 application.properties에 명시, 개발 중에만 하드코드로 넣어주어야함
    @Value("${jwt.secret:mySecretKeyForJwtTokenGenerationAndValidation123456789}")
    private String secretKey;

    //추후에 application.properties에 명시, 개발 중에만 하드코드로 넣어주기,refresh 토큰이 없기때문에 적당히 길게 설정
    @Value("${jwt.expiration:14400}")
    private Long jwtExpiration;

    /**
     * JWT 액세스 토큰 생성
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @return JWT 액세스 토큰
     */
    public String generateAccessToken(Long userId, String email){
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "ACCESS");

        return createToken(claims, email, jwtExpiration);
    }

    /**
     * 토큰에서 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public String getUserIdFromToken(String token){
        Claims claims = getClaimsFromToken(token);
        // Long/Integer 타입을 String으로 안전하게 변환
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Long) {
            return userIdObj.toString();
        } else if (userIdObj instanceof Integer) {
            return userIdObj.toString();
        }
        return String.valueOf(userIdObj);
    }

    /**
     * 토큰에서 이메일 추출
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getUserEmailFromToken(String token){
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 토큰 만료 시간 추출
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public Date getExpirationDateFromToken(String token){
        return getClaimsFromToken(token).getExpiration();
    }

    /**
     * 토큰이 만료되었는지 확인
     * @param token JWT 토큰
     * @return 만료 여부
     */
    public boolean isTokenExpired(String token){
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;  // 이미 만료된 토큰
        }
    }

    /**
     * 토큰 유효성 검증
     * @param token JWT 토큰
     * @param email 사용자 이메일
     * @return 유효성 여부
     */
    public boolean validateToken(String token, String email){
        try{
            final String tokenEmail = getUserEmailFromToken(token);
            return (tokenEmail.equals(email) && !isTokenExpired(token));
        }catch (Exception e){
            return false;
        }
    }

    /**
     * 토큰 타입 확인 (ACCESS만 사용)
     * @param token JWT 토큰
     * @return 토큰 타입
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 액세스 토큰인지 확인
     * @param token JWT 토큰
     * @return 액세스 토큰 여부
     */
    public boolean isAccessToken(String token){
        return "ACCESS".equals(getTokenType(token));
    }

    private String createToken(Map<String, Object>claims, String subject, Long expiration){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime()+expiration*1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey getSigningKey(){
        byte [] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims는 반환 (만료 체크용)
            return e.getClaims();
        }
    }


}
