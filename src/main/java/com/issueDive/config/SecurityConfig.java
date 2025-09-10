package com.issueDive.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService; // DB 기반 인증
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 필터


    // 공개적으로 접근 가능한 URL 목록
    private static final String[] PUBLIC_URLS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/actuator/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_URLS).permitAll() // 공개 URL은 모두 허용
                        .requestMatchers("/auth/signup", "/auth/signup-only", "/auth/login").permitAll() // 회원가입, 로그인만 공개
                        .requestMatchers("/auth/logout").authenticated()        // 로그아웃은 인증 필요 (블랙리스트 처리)
                        .requestMatchers("/auth/refresh").permitAll()           // 리프레시 토큰은 공개 (토큰 자체로 검증)
                        .requestMatchers("/auth/users/**").authenticated()      // 사용자 정보 조회는 인증 필요
                        .requestMatchers(HttpMethod.GET, "/issues", "/issues/**").permitAll()   // 이슈 조회(GET)는 모두 허용
                        .requestMatchers(HttpMethod.GET, "/labels", "/labels/**").permitAll()   // 라벨 조회(GET)도 모두 허용
                        // 888 변경: 추가 공개 엔드포인트들
                        .requestMatchers(HttpMethod.GET, "/issues/*/comments").permitAll()      // 댓글 목록 조회 허용
                        .requestMatchers(HttpMethod.GET, "/issues/*/comments/count").permitAll() // 댓글 수 조회 허용
                        .requestMatchers(HttpMethod.GET, "/issues/*/navigation").permitAll()    // 이슈 네비게이션 조회 허용// 3. 라벨 조회(GET)도 모두 허용
                        .anyRequest().authenticated()             // 나머지는 인증 필요
                )
                .formLogin(formLogin -> formLogin.disable())         // 폼 로그인 비활성화 (서버 사이드 렌더링 사용X)
                .logout(logout -> logout.disable());                    // 로그아웃 비활성화 (서버에 로그인 상태 저장X: Stateless)
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 서버 주소 허용
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:5174"));
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // 모든 헤더 허용
//        configuration.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
        configuration.addAllowedHeader("*");
        // 자격 증명(쿠키 등) 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로("/**")에 대해 위에서 정의한 CORS 정책 적용
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("test")
                .password(passwordEncoder.encode("test"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

     */

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}