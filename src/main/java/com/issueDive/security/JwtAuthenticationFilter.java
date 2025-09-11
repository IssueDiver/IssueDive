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

                if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("블랙리스트 토큰 사용 시도");
                     setErrorResponse(response, "토큰이 무효화되었습니다.");
                     return;
                } else {
                    String email = jwtUtil.getUserEmailFromToken(jwt);

                    // 토큰이 유효한 경우에만 SecurityContext에 인증 정보 저장
                    if (jwtUtil.validateToken(jwt, email)) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("사용자 {} 인증 성공", email);
                    }
                    // ** 토큰이 유효하지 않은 경우(else), 아무것도 하지 않고 넘어감
                }
            }
        } catch (Exception e) {
            // 로그만 남기고 넘김
            log.error("JWT 토큰 처리 중 오류 발생 (요청은 계속 진행): {}", e.getMessage());
        }

        // 모든 경우에 대해 다음 필터로 요청 전달
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
            """, message, java.time.LocalDateTime.now()));
    }

    /**
     * 공개 URL에 대해서는 필터를 적용하지 않음 (9월1일 변경 - RefreshToken URL 제거)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/signup") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/refresh") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs")||
                path.startsWith("/actuator");
    }
}
