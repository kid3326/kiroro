package com.retaildashboard.config;

import com.retaildashboard.domain.User;
import com.retaildashboard.exception.AuthenticationException;
import com.retaildashboard.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 세션 기반 인증 필터.
 * X-Session-Token 헤더에서 세션 토큰을 추출하고,
 * AuthenticationService.validateSession()을 사용하여 세션을 검증합니다.
 *
 * 인증 성공 시 SecurityContext에 사용자 정보를 설정합니다.
 *
 * Requirements: 2.1, 2.2, 15.1
 */
@RequiredArgsConstructor
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private final AuthenticationService authenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(SESSION_TOKEN_HEADER);

        if (token != null && !token.isBlank()) {
            try {
                User user = authenticationService.validateSession(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("세션 인증 성공: userId={}, role={}", user.getId(), user.getRole());

            } catch (AuthenticationException e) {
                log.debug("세션 인증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
