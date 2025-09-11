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

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtUtil jwtUtil;

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
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(true);
        given(customUserDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(USER_EMAIL);
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
    @DisplayName("유효하지 않은 토큰은 에러 응답 없이 필터를 통과해야 함")
    void doFilterInternal_InvalidToken_ShouldPass() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN)).willReturn(USER_EMAIL);
        given(jwtUtil.validateToken(VALID_TOKEN, USER_EMAIL)).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // ======================= 변경된 검증 로직 =======================
        // 1. response 객체와는 아무런 상호작용이 없어야 함
        verify(response, never()).setContentType(anyString());
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).getWriter();

        // 2. 대신, filterChain.doFilter()가 1번 호출되어 요청이 계속 진행되어야 함
        verify(filterChain, times(1)).doFilter(request, response);

        // 3. 인증 정보는 등록되지 않아야 함
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // ==========================================================
    }

    @Test
    @DisplayName("JWT 파싱 중 예외가 발생해도 에러 응답 없이 필터를 통과해야 함")
    void doFilterInternal_ExceptionThrown_ShouldPass() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + VALID_TOKEN);
        given(jwtUtil.getUserEmailFromToken(VALID_TOKEN))
                .willThrow(new RuntimeException("JWT 파싱 오류"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // ======================= 변경된 검증 로직 =======================
        // 1. response 객체와는 아무런 상호작용이 없어야 함
        verify(response, never()).setContentType(anyString());
        verify(response, never()).setStatus(anyInt());

        // 2. 대신, filterChain.doFilter()가 1번 호출되어 요청이 계속 진행되어야 함
        verify(filterChain, times(1)).doFilter(request, response);
        // ==========================================================
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
    @DisplayName("Bearer 뒤에 토큰이 없는 경우 필터를 통과해야 함")
    void doFilterInternal_BearerWithoutToken_Pass() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer ");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // ======================= 변경된 검증 로직 =======================
        // 1. response 객체와는 아무런 상호작용이 없어야 함
        verify(response, never()).setContentType(anyString());
        verify(response, never()).setStatus(anyInt());

        // 2. 다음 필터로 요청이 넘어가야 함
        verify(filterChain, times(1)).doFilter(request, response);

        // 3. 인증 정보는 등록되지 않아야 함
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // ==========================================================
    }
}