package com.issueDive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트 전용 Redis 설정
 * TestContainers를 사용한 Redis 인스턴스 관리
 */
@TestConfiguration
@Profile("test")
public class TestRedisConfiguration {
    private static GenericContainer<?> redis;

    static {
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();
    }

    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig =
                new RedisStandaloneConfiguration(redis.getHost(), redis.getFirstMappedPort());
        return new JedisConnectionFactory(redisConfig);
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String 직렬화 사용
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        return template;
    }
}
