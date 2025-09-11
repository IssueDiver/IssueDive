package com.issueDive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * 9월 10일 최종 - Token Blacklist Service
 * Redis를 사용하여 로그아웃된 JWT 토큰을 블랙리스트로 관리
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * 9월 10일 최종 - 토큰을 블랙리스트에 추가
     */
    public void addToBlacklist(String token, Date expirationDate) {
        try {
            String key = BLACKLIST_PREFIX + token;
            long expirationMillis = expirationDate.getTime() - System.currentTimeMillis();

            if (expirationMillis > 0) {
                stringRedisTemplate.opsForValue().set(
                        key,
                        "blacklisted",
                        Duration.ofMillis(expirationMillis)
                );
                log.info("9월 10일 최종 - 토큰 블랙리스트 추가됨");
            }
        } catch (Exception e) {
            log.error("9월 10일 최종 - 토큰 블랙리스트 추가 실패: {}", e.getMessage());
        }
    }

    /**
     * 9월 10일 최종 - 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("9월 10일 최종 - 토큰 블랙리스트 확인 실패: {}", e.getMessage());
            return true; // Redis 장애 시 보안을 위해 true 반환
        }
    }
}
