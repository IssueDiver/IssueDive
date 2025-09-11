package com.issueDive.security;

import com.issueDive.service.TokenBlacklistService;
import com.issueDive.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    @Autowired(required = false)  // 9월 10일
    private TokenBlacklistService tokenBlacklistService; // 9월 10일 최종

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 9월 10일 최종 - 블랙리스트 체크 추가
                if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("9월 10일 최종 - 블랙리스트 토큰 사용 시도");
                    setErrorResponse(response, "토큰이 무효화되었습니다.");
                    return;
                }
                // JWT에서 이메일 추출
                String email = jwtUtil.getUserEmailFromToken(jwt);

                // 9월1일 변경 - AccessToken만 사용하므로 타입 체크 제거

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
            setErrorResponse(response, "토큰 처리 중 오류가 발생했습니다.");
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

    /**
     * 공개 URL에 대해서는 필터를 적용하지 않음 (9월1일 변경 - RefreshToken URL 제거)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/signup") ||
                path.startsWith("/auth/login") ||
                path.startsWith("/auth/refresh") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs")||
                path.startsWith("/actuator");
    }
}
