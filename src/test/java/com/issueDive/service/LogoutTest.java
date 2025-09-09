package com.issueDive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TokenBlacklistService 단위 테스트
 * Redis 모킹하여 테스트
 */
@ExtendWith(MockitoExtension.class)
public class LogoutTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlackListService tokenBlackListService;

    private String testToken = "eyJhbGciOiJIUzI1NiJ9.testtoken";
    private String blacklistKey = "blacklist:" + testToken;

    @BeforeEach
    void setUp() {
        // TokenBlacklistService 수동 생성 (Mock 주입)
        tokenBlackListService = new TokenBlackListService(redisTemplate);
    }

    @Test
    @DisplayName("토큰을 블랙리스트에 추가")
    void addToBlacklistTest() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long expirationTime = 3600L; // 1시간

        // When
        tokenBlackListService.addToBlackList(testToken, expirationTime);

        // Then
        verify(valueOperations).set(
                eq(blacklistKey),
                eq("blacklisted"),
                eq(expirationTime),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("블랙리스트에 있는 토큰 확인 - 존재하는 경우")
    void isBlacklistedWhenTokenExists() {
        // Given
        when(redisTemplate.hasKey(blacklistKey)).thenReturn(true);

        // When
        boolean result = tokenBlackListService.isBlackListed(testToken);

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey(blacklistKey);
    }

    @Test
    @DisplayName("블랙리스트에 있는 토큰 확인 - 존재하지 않는 경우")
    void isBlacklistedWhenTokenNotExists() {
        // Given
        when(redisTemplate.hasKey(blacklistKey)).thenReturn(false);

        // When
        boolean result = tokenBlackListService.isBlackListed(testToken);

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey(blacklistKey);
    }

    @Test
    @DisplayName("블랙리스트에서 토큰 제거")
    void removeFromBlacklistTest() {
        // Given
        when(redisTemplate.delete(blacklistKey)).thenReturn(true);

        // When
        tokenBlackListService.removeFromBlackList(testToken);

        // Then
        verify(redisTemplate).delete(blacklistKey);
    }

    @Test
    @DisplayName("만료 시간이 0일 때 블랙리스트 추가")
    void addToBlacklistWithZeroExpiration() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long expirationTime = 0L;

        // When
        tokenBlackListService.addToBlackList(testToken, expirationTime);

        // Then
        verify(valueOperations).set(
                eq(blacklistKey),
                eq("blacklisted"),
                eq(0L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("null 토큰 처리 - 블랙리스트 확인")
    void isBlacklistedWithNullToken() {
        // Given
        String nullKey = "blacklist:null";
        when(redisTemplate.hasKey(nullKey)).thenReturn(false);

        // When
        boolean result = tokenBlackListService.isBlackListed(null);

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey(nullKey);
    }

    @Test
    @DisplayName("빈 토큰 처리 - 블랙리스트 확인")
    void isBlacklistedWithEmptyToken() {
        // Given
        String emptyKey = "blacklist:";
        when(redisTemplate.hasKey(emptyKey)).thenReturn(false);

        // When
        boolean result = tokenBlackListService.isBlackListed("");

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey(emptyKey);
    }

    @Test
    @DisplayName("긴 만료 시간으로 블랙리스트 추가")
    void addToBlacklistWithLongExpiration() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long expirationTime = 86400L; // 24시간

        // When
        tokenBlackListService.addToBlackList(testToken, expirationTime);

        // Then
        verify(valueOperations).set(
                eq(blacklistKey),
                eq("blacklisted"),
                eq(86400L),
                eq(TimeUnit.SECONDS)
        );
    }
}