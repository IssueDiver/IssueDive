package com.issueDive.security;

import com.issueDive.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.issueDive.service.RedisService;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private UserDetails userDetails;
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = User.builder()
                .username(USER_EMAIL)
                .password("password")
                .authorities(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증 성공")
    void doFilterInternal_ValidToken_Success() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);
        given(redisService.isBlacklisted(VALID_TOKEN)).willReturn(false);
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.isAccessToken(VALID_TOKEN)).willReturn(true);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(USER_EMAIL);
    }

    // 블랙리스트에 있는 토큰 테스트 추가
    @Test
    @DisplayName("블랙리스트에 등록된 토큰으로 인증 실패")
    void doFilterInternal_BlacklistedToken_Failure() throws ServletException, IOException {
        // given
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);
        given(redisService.isBlacklisted(VALID_TOKEN)).willReturn(true); // 블랙리스트에 존재
        given(response.getWriter()).willReturn(writer);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(stringWriter.toString()).contains("이미 로그아웃된 토큰입니다.");
    }

    // 리프레시 토큰으로 인증 시도 실패 테스트 추가
    @Test
    @DisplayName("리프레시 토큰으로 인증 시도 시 실패")
    void doFilterInternal_RefreshToken_Failure() throws ServletException, IOException {
        // given
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);
        given(redisService.isBlacklisted(VALID_TOKEN)).willReturn(false);
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.isAccessToken(VALID_TOKEN)).willReturn(false); // 액세스 토큰이 아님
        given(response.getWriter()).willReturn(writer);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(stringWriter.toString()).contains("유효하지 않은 토큰 타입입니다.");
    }

    @Test
    @DisplayName("Authorization 헤더가 없는 경우 필터 통과")
    void doFilterInternal_NoAuthHeader_Pass() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtUtil, never()).getUserEmailFromToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 토큰 처리")
    void doFilterInternal_NoBearerPrefix_Pass() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn(VALID_TOKEN);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtUtil, never()).getUserEmailFromToken(anyString());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 인증 실패")
    void doFilterInternal_InvalidToken_Failure() throws ServletException, IOException {
        // given
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);
        given(redisService.isBlacklisted(VALID_TOKEN)).willReturn(false);
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.isAccessToken(VALID_TOKEN)).willReturn(true);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(false);
        given(response.getWriter()).willReturn(writer);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(stringWriter.toString()).contains("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("JWT 파싱 중 예외 발생 처리")
    void doFilterInternal_ExceptionThrown_Handled() throws ServletException, IOException {
        // given
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);
        given(redisService.isBlacklisted(VALID_TOKEN)).willReturn(false);
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN))
                .willThrow(new RuntimeException("JWT 파싱 오류"));
        given(response.getWriter()).willReturn(writer);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(stringWriter.toString()).contains("토큰 처리 중 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("공개 URL은 필터를 적용하지 않음 - /auth/signup")
    void shouldNotFilter_PublicUrl_Signup() {
        // given
        given(request.getRequestURI()).willReturn("/auth/signup");

        // when
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("공개 URL은 필터를 적용하지 않음 - /auth/login")
    void shouldNotFilter_PublicUrl_Login() {
        // given
        given(request.getRequestURI()).willReturn("/auth/login");

        // when
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isTrue();
    }

    // /auth/refresh 경로 테스트 추가
    @Test
    @DisplayName("공개 URL은 필터를 적용하지 않음 - /auth/refresh")
    void shouldNotFilter_PublicUrl_Refresh() {
        // given
        given(request.getRequestURI()).willReturn("/auth/refresh");

        // when
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isTrue();
    }


    @Test
    @DisplayName("공개 URL은 필터를 적용하지 않음 - /swagger-ui")
    void shouldNotFilter_PublicUrl_Swagger() {
        // given
        given(request.getRequestURI()).willReturn("/swagger-ui/index.html");

        // when
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("보호된 URL은 필터를 적용함")
    void shouldNotFilter_ProtectedUrl() {
        // given
        given(request.getRequestURI()).willReturn("/issues");

        // when
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isFalse();
    }

    @Test
    @DisplayName("이미 인증된 컨텍스트가 있는 경우 건너뛰기")
    void doFilterInternal_AlreadyAuthenticated_Skip() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);

        // 이미 인증 컨텍스트가 설정되어 있는 상황 시뮬레이션
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities())
        );

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtUtil, never()).getUserEmailFromToken(anyString());
    }

    @Test
    @DisplayName("빈 Authorization 헤더 처리")
    void doFilterInternal_EmptyAuthHeader_Pass() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtUtil, never()).getUserEmailFromToken(anyString());
    }

    @Test
    @DisplayName("Bearer 뒤에 토큰이 없는 경우 처리")
    void doFilterInternal_BearerWithoutToken_Pass() throws ServletException, IOException {
        // given
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        // 9월 2일 변경: response.getWriter() mock을 먼저 설정 - NullPointerException 방지
        given(response.getWriter()).willReturn(writer);
        given(request.getHeader("Authorization")).willReturn("Bearer ");
        // 9월 2일 변경: request.getRequestURI() stubbing 제거 - 사용되지 않음
        // given(request.getRequestURI()).willReturn("/issues");

        // 9월 2일 변경: jwtUtil mock 추가 - Bearer 뒤 빈 토큰 파싱 시 null 반환
        given(jwtUtil.getUserEmailFromToken("")).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // 9월 2일 변경: 토큰이 빈 문자열이므로 에러 응답 검증으로 변경
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(stringWriter.toString()).contains("유효하지 않은 토큰 타입입니다.");
    }
}
