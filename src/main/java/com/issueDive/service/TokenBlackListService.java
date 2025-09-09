package com.issueDive.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * JWT 토큰 블랙리스트 관리 서비스
 * 로그아웃된 토큰을 Redis에 저장하여 무효화 처리
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlackListService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * 토큰을 블랙리스트에 추가
     * @param token JWT 토큰
     * @param expirationTimeInSeconds 토큰 만료까지 남은 시간(초)
     */

    public void addToBlackList(String token, long expirationTimeInSeconds){
        String key = BLACKLIST_PREFIX + token;

        // Redis에 토큰 저장 (TTL = 토큰의 남은 유효시간)
        // 토큰이 만료되면 자동으로 Redis에서도 삭제됨
        redisTemplate.opsForValue().set(
                key,
                "blacklisted",
                expirationTimeInSeconds,
                TimeUnit.SECONDS
        );

        log.info("토근이 블래리스트에 추가됨. 만료시간 {}초", expirationTimeInSeconds);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token JWT 토큰
     * @return 블랙리스트 포함 여부
     */
    public boolean isBlackListed(String token){
        String key = BLACKLIST_PREFIX+token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * 블랙리스트에서 토큰 제거 (수동 제거가 필요한 경우)
     * @param token JWT 토큰
     */
    public void removeFromBlackList(String token){
        String key = BLACKLIST_PREFIX+token;
        redisTemplate.delete(key);
        log.info("토근이 블랙리스트에서 제거되었습니다.");
    }

}
