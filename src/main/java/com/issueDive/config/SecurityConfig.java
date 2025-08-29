package com.issueDive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 공개적으로 접근 가능한 URL 목록
    private static final String[] PUBLIC_URLS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/auth/signup"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_URLS).permitAll() // 공개 URL은 모두 허용
                        .anyRequest().authenticated()             // 나머지는 인증 필요
                )
                .formLogin(formLogin -> formLogin             // 폼 로그인 사용
                        .defaultSuccessUrl("/")
                        .permitAll()
                )
                .logout(logout -> logout                      // 로그아웃 사용
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 서버 주소 허용
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
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

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("test")
                .password(passwordEncoder.encode("test"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

}