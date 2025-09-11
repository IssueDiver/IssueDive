package com.issueDive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 9월 10일 최종 - TokenBlacklistService 테스트
 *
 */
@ExtendWith(MockitoExtension.class)
public class TokenBlacklistServiceTest {
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;


    @Test
    @DisplayName("9월 10일 최종 - 토큰 블랙리스트 추가")
    void addToBlacklist_Success() {
        // given
        String token = "test.token";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        tokenBlacklistService.addToBlacklist(token, expirationDate);

        // then
        verify(valueOperations).set(anyString(), eq("blacklisted"), any(Duration.class));
    }

    @Test
    @DisplayName("9월 10일 최종 - 블랙리스트 확인")
    void isBlacklisted_Success() {
        // given
        String token = "test.token";
        given(stringRedisTemplate.hasKey(anyString())).willReturn(true);

        // when
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // then
        assertThat(result).isTrue();
    }
}
