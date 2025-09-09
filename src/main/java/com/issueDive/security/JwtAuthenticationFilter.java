package com.issueDive.security;

import com.issueDive.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

import com.issueDive.service.TokenBlackListService;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlackListService tokenBlackListService;
    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/auth/signup", "/auth/login", "/swagger-ui", "/v3/api-docs"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request != null ? request.getRequestURI() : null; // 9월9일 수정: NPE 방어
        if (uri == null) return false; // 안전하게 필터 적용  // 9월9일 수정
        return PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 블랙리스트 체크 추가 (여기만 새로 추가)
                if (tokenBlackListService.isBlackListed(jwt)) {
                    log.warn("Attempted to use blacklisted token");
                    setErrorResponse(response, "유효하지 않은 토큰입니다.");
                    return;
                }

                // JWT에서 이메일 추출
                String email = jwtUtil.getUserEmailFromToken(jwt);

                // AccessToken만 사용하므로 타입 체크 제거

                // 토큰 유효성 검증
                if (jwtUtil.validateToken(jwt, email)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("사용자 {} 인증 성공", email);
                } else {
                    log.warn("유효하지 않은 JWT 토큰: {}", email);
                    setErrorResponse(response, "유효하지 않은 토큰입니다.");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생", e);
            setErrorResponse(response, "유효하지 않은 토큰입니다.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 JWT 토큰 추출
     * Authorization: Bearer <token>
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        return null;
    }

    /**
     * 에러 응답 설정
     */
    private void setErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(String.format("""
            {
                "success": false,
                "error": {
                    "code": "Unauthorized",
                    "message": "%s"
                },
                "timestamp": "%s"
            }
            """, message, java.time.LocalDateTime.now().toString()));
    }
}
