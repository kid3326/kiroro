package com.retaildashboard.config;

import com.retaildashboard.service.AuthenticationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정.
 * 세션 기반 인증, CORS, 엔드포인트 접근 제어를 구성합니다.
 *
 * - /api/v1/auth/** 엔드포인트는 인증 없이 접근 가능
 * - 그 외 /api/v1/** 엔드포인트는 인증 필요
 * - CORS: 프론트엔드 origin (localhost:3000) 허용
 *
 * Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 15.1
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationService authenticationService;

    public SecurityConfig(@Lazy AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * BCryptPasswordEncoder Bean (cost factor = 12).
     * Requirement 2.6: bcrypt with cost factor 12.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Spring Security FilterChain 설정.
     * - CSRF 비활성화 (REST API이므로)
     * - CORS 설정 적용
     * - 세션 관리: STATELESS (자체 세션 토큰 사용)
     * - /api/v1/auth/** 인증 없이 허용
     * - /api/v1/** 인증 필요
     * - SessionAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/mock/api/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(
                        sessionAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * SessionAuthenticationFilter Bean.
     * X-Session-Token 헤더를 통한 세션 기반 인증을 처리합니다.
     */
    @Bean
    public SessionAuthenticationFilter sessionAuthenticationFilter() {
        return new SessionAuthenticationFilter(authenticationService);
    }

    /**
     * CORS 설정.
     * 프론트엔드 개발 서버(localhost:3000)에서의 요청을 허용합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Session-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
