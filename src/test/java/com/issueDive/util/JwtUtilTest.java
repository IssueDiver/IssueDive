package com.issueDive.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
//
@ExtendWith(MockitoExtension.class)
public class JwtUtilTest {
    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String SECRET_KEY = "mySecretKeyForJwtTokenGenerationAndValidation123456789";
    private static final Long EXPIRATION = 14400L; // 4시간 (초 단위)
    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // @Value 필드 수동 주입
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", EXPIRATION);
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void generateAccessToken_Success() {
        // when
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 3개 부분으로 구성
    }

    @Test
    @DisplayName("토큰에서 사용자 ID 추출 성공")
    void getUserIdFromToken_Success() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        String userId = jwtUtil.getUserIdFromToken(token);

        // then
        assertThat(userId).isEqualTo(USER_ID.toString());
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 성공")
    void getUserEmailFromToken_Success() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        String email = jwtUtil.getUserEmailFromToken(token);

        // then
        assertThat(email).isEqualTo(USER_EMAIL);
    }

    @Test
    @DisplayName("토큰 만료 시간 추출 성공")
    void getExpirationDateFromToken_Success() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);

        // then
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate).isAfter(new Date());
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_ValidToken_Success() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        boolean isValid = jwtUtil.validateToken(token, USER_EMAIL);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 이메일로 토큰 검증 실패")
    void validateToken_WrongEmail_Failure() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        boolean isValid = jwtUtil.validateToken(token, "wrong@example.com");

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증")
    void isTokenExpired_ExpiredToken() throws InterruptedException {
        // given
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1L); // 1초 후 만료
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // 토큰이 만료될 때까지 대기
        Thread.sleep(1500);

        // when
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("만료되지 않은 토큰 검증")
    void isTokenExpired_ValidToken() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("액세스 토큰 타입 확인")
    void isAccessToken_Success() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        boolean isAccessToken = jwtUtil.isAccessToken(token);

        // then
        assertThat(isAccessToken).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 파싱 실패")
    void parseInvalidToken_ThrowsException() {
        // given
        String invalidToken = "invalid.token.format";

        // when & then
        assertThatThrownBy(() -> jwtUtil.getUserEmailFromToken(invalidToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("null 토큰 검증 실패")
    void validateToken_NullToken_Failure() {
        // when
        boolean isValid = jwtUtil.validateToken(null, USER_EMAIL);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    void validateToken_EmptyToken_Failure() {
        // when
        boolean isValid = jwtUtil.validateToken("", USER_EMAIL);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰 타입 추출 성공")
    void getTokenType_Success() {
        // given
        String token = jwtUtil.generateAccessToken(USER_ID, USER_EMAIL);

        // when
        String tokenType = jwtUtil.getTokenType(token);

        // then
        assertThat(tokenType).isEqualTo("ACCESS");
    }
}
