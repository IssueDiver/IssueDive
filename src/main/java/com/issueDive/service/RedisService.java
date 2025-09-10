package com.issueDive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    // 리프레시 토큰 저장
    public void saveRefreshToken(String userEmail, String refreshToken, Long expirationTime) {
        String key = "refresh:" + userEmail;
        redisTemplate.opsForValue().set(key, refreshToken, expirationTime, TimeUnit.SECONDS);
        log.debug("Refresh token saved for user: {}", userEmail);
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(String userEmail) {
        String key = "refresh:" + userEmail;
        return redisTemplate.opsForValue().get(key);
    }

    // 리프레시 토큰 삭제 (로그아웃 시)
    public void deleteRefreshToken(String userEmail) {
        String key = "refresh:" + userEmail;
        redisTemplate.delete(key);
        log.debug("Refresh token deleted for user: {}", userEmail);
    }

    // 액세스 토큰 블랙리스트 추가
    public void addToBlacklist(String accessToken, Long expirationTime) {
        String key = "blacklist:" + accessToken;
        redisTemplate.opsForValue().set(key, "true", expirationTime, TimeUnit.SECONDS);
        log.debug("Access token added to blacklist");
    }

    // 9토큰이 블랙리스트에 있는지 확인
    public boolean isBlacklisted(String accessToken) {
        String key = "blacklist:" + accessToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 리프레시 토큰 유효성 검증
    public boolean validateRefreshToken(String userEmail, String refreshToken) {
        String storedToken = getRefreshToken(userEmail);
        return refreshToken != null && refreshToken.equals(storedToken);
    }
}
