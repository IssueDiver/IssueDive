package com.issueDive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Spring Security 활성화
public class SecurityConfig {

    // 인증 없이 접근을 허용할 경로 목록
    private static final String[] PUBLIC_URLS = {
            "/auth/signup",         // 회원가입
            //"/auth/login",          // 로그인
            "/swagger-ui/**",       // Swagger UI 페이지
            "/v3/api-docs/**",      // Swagger API 문서
            "/swagger-resources/**" // Swagger 리소스
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 요청에 대한 인가 규칙 설정
                .authorizeHttpRequests(authorize -> authorize
                        // PUBLIC_URLS는 인증 없이 접근 허용
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 2. 폼 기반 로그인 설정
                .formLogin(formLogin -> formLogin
                        // 사용자가 만든 별도의 로그인 페이지가 있다면 .loginPage("/login-page") 등으로 설정
                        // 설정하지 않으면 Spring Security가 기본 로그인 페이지를 제공 (/login)
                        .defaultSuccessUrl("/") // 로그인 성공 시 이동할 기본 URL
                        .permitAll() // 로그인 과정은 누구나 접근 가능해야 함
                )

                // 3. 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("/login") // 로그아웃 성공 시 이동할 URL
                        .invalidateHttpSession(true) // 로그아웃 시 세션 무효화
                );

        // 4. CSRF 설정은 기본적으로 활성화되어 있으므로 별도로 disable() 하지 않음
        // (세션 방식에서는 CSRF 보호를 활성화하는 것이 보안상 안전)
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // 4. PUBLIC_URLS에 해당하는 경로는 보안 필터를 무시하도록 설정
        return (web) -> web.ignoring().requestMatchers(PUBLIC_URLS);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 암호화를 위한 BCryptPasswordEncoder 등록
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("tuser")
                .password(passwordEncoder.encode("tpw"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }


//        // JWT 기반 인증 사용 시
//        @Bean
//        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                // 1. CSRF(Cross-Site Request Forgery) 비활성화
//                .csrf(csrf -> csrf.disable())
//
//                // 2. 세션 관리 정책 설정: Stateless (세션을 사용 안할 경우)
//                // (JWT 기반 인증을 사용할 예정이라면 Stateless가 적합)
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//
//                // 3. HTTP 요청에 대한 인가 규칙 설정
//                .authorizeHttpRequests(authorize -> authorize
//                        // 위에서 정의한 PUBLIC_URLS는 인증 없이 접근 허용
//                        .requestMatchers(PUBLIC_URLS).permitAll()
//                        // 그 외 모든 요청은 인증된 사용자만 접근 가능
//                        .anyRequest().authenticated()
//                )
//
//                // 4. Form 로그인 방식 비활성화 (REST API이므로)
//                .formLogin(formLogin -> formLogin.disable())
//
//                // 5. HTTP Basic 인증 방식 비활성화 (REST API이므로)
//                .httpBasic(httpBasic -> httpBasic.disable());
//
//        return http.build();
//    }
}