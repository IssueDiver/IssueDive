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

    //추후에 application.properties에 명시, 개발 중에만 하드코드로 넣어주기
    @Value("${jwt.secret:mySecretKeyForJwtTokenGenerationAndValidation123456789}")
    private String secretKey;

    //추후에 application.properties에 명시, 개발 중에만 하드코드로 넣어주기,refresh 토큰이 없기때문에 적당히 길게 설정
    @Value("${jwt.expiration:14400}")
    private Long jwtExpiration;

    // 리프레시 토큰 만료 시간 추가 (7일)
    @Value("${jwt.refresh.expiration:604800}")
    private Long refreshExpiration;

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

    // 리프레시 토큰 생성 메서드 추가
    /**
     * JWT 리프레시 토큰 생성
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @return JWT 리프레시 토큰
     */
    public String generateRefreshToken(Long userId, String email){
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "REFRESH");

        return createToken(claims, email, refreshExpiration);
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
        // ExpiredJwtException 처리 추가
        try {
            return getClaimsFromToken(token).getExpiration();
        } catch (ExpiredJwtException e) {
            // 4번 변경 - 만료된 토큰에서도 만료 시간 추출
            return e.getClaims().getExpiration();
        }
    }

    /**
     * 토큰이 만료되었는지 확인
     * @param token JWT 토큰
     * @return 만료 여부
     */
    public boolean isTokenExpired(String token){
        // 완전히 새로운 방식으로 만료 확인
        try {
            // 파싱 시도만으로 만료 확인
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return false; //  파싱 성공 = 만료되지 않음
        } catch (ExpiredJwtException e) {
            return true;  // ExpiredJwtException = 만료됨
        } catch (Exception e) {
            // 다른 예외는 만료와 무관하므로 false 반환
            System.err.println("Error checking token expiration: " + e.getMessage());
            return false;
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
            // 2번째 변경 - 디버깅 로그 추가
            boolean emailMatches = tokenEmail.equals(email);
            boolean notExpired = !isTokenExpired(token);

            System.out.println("Token validation - Email from token: " + tokenEmail);
            System.out.println("Token validation - Email to match: " + email);
            System.out.println("Token validation - Email matches: " + emailMatches);
            System.out.println("Token validation - Not expired: " + notExpired);

            return emailMatches && notExpired;
        }catch (Exception e){
            System.err.println("Token validation error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 타입 확인 (ACCESS만 사용)
     * @param token JWT 토큰
     * @return 토큰 타입
     */
    public String getTokenType(String token) {
        // 2번째 변경 - null 체크 및 예외 처리 추가
        try {
            Claims claims = getClaimsFromToken(token);
            String type = claims.get("type", String.class);
            System.out.println("Token type from claims: " + type);
            return type;
        } catch (Exception e) {
            System.err.println("Failed to get token type: " + e.getMessage());
            return null;
        }
    }

    /**
     * 액세스 토큰인지 확인
     * @param token JWT 토큰
     * @return 액세스 토큰 여부
     */
    public boolean isAccessToken(String token){
// 2번째 변경 - 디버깅 로그 추가
        String type = getTokenType(token);
        boolean isAccess = "ACCESS".equals(type);
        System.out.println("Is Access Token? " + isAccess + " (type: " + type + ")");
        return isAccess;
    }

    //  리프레시 토큰 확인 메서드 추가
    /**
     * 리프레시 토큰인지 확인
     * @param token JWT 토큰
     * @return 리프레시 토큰 여부
     */
    public boolean isRefreshToken(String token){
        return "REFRESH".equals(getTokenType(token));
    }

    // 9월10일 수정 - 남은 만료 시간 계산 메서드 추가
    /**
     * 토큰의 남은 만료 시간 계산 (초 단위)
     * @param token JWT 토큰
     * @return 남은 만료 시간 (초)
     */
    public Long getRemainingExpirationTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            Date now = new Date();
            long diff = expiration.getTime() - now.getTime();
            return diff > 0 ? diff / 1000 : 0;
        } catch (Exception e) {
            return 0L;
        }
    }

    //리프레시 토큰 만료 시간 getter 추가
    public Long getRefreshExpiration() {
        return refreshExpiration;
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
            // 만료된 토큰이어도 Claims는 반환 (로그 제거)
            return e.getClaims();
        } catch (MalformedJwtException e) {
            // MalformedJwtException은 그대로 던짐 (로그 제거)
            throw e;
        }}

    // 2번째 변경 - 토큰 디버깅용 메서드 추가
    /**
     * 토큰 정보 출력 (디버깅용)
     * @param token JWT 토큰
     */
    public void debugToken(String token) {
        try {
            System.out.println("=== Token Debug Info ===");
            Claims claims = getClaimsFromToken(token);
            System.out.println("Subject (email): " + claims.getSubject());
            System.out.println("User ID: " + claims.get("userId"));
            System.out.println("Type: " + claims.get("type"));
            System.out.println("Issued At: " + claims.getIssuedAt());
            System.out.println("Expiration: " + claims.getExpiration());
            System.out.println("Is Expired: " + isTokenExpired(token));
            System.out.println("========================");
        } catch (Exception e) {
            System.err.println("Token debug failed: " + e.getMessage());
        }
    }
    }
