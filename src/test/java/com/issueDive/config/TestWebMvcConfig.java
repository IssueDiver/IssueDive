package com.issueDive.config;

import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.service.RedisService;
import com.issueDive.util.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestWebMvcConfig {
    @Bean
    @Primary
    public RedisService redisService() {
        return mock(RedisService.class);
    }

    @Bean
    @Primary
    public JwtUtil mockJwtUtil() {
        return mock(JwtUtil.class);
    }

    @Bean
    @Primary
    public CustomUserDetailsService mockCustomUserDetailsService() {
        return mock(CustomUserDetailsService.class);
    }
}
