package com.retaildashboard.controller;

import com.retaildashboard.domain.User;
import com.retaildashboard.dto.LoginRequest;
import com.retaildashboard.dto.LoginResponse;
import com.retaildashboard.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 인증 REST API 컨트롤러.
 * 로그인, 로그아웃, 세션 확인 엔드포인트를 제공합니다.
 *
 * Requirements: 2.1, 2.2, 2.7
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private final AuthenticationService authenticationService;

    /**
     * 사용자 로그인.
     * 클라이언트 IP를 요청에서 추출하여 세션에 기록합니다.
     *
     * @param request 로그인 요청 (username, password)
     * @param httpRequest HTTP 요청 (IP 추출용)
     * @return 로그인 응답 (token, 사용자 정보, 만료 시간)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        request.setIpAddress(extractClientIp(httpRequest));
        LoginResponse response = authenticationService.login(request);

        log.info("로그인 성공: username={}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 로그아웃.
     * X-Session-Token 헤더에서 세션 토큰을 추출하여 세션을 종료합니다.
     *
     * @param token 세션 토큰
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(SESSION_TOKEN_HEADER) String token) {

        authenticationService.logout(token);

        log.info("로그아웃 완료");
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다"));
    }

    /**
     * 세션 유효성 확인.
     * X-Session-Token 헤더에서 세션 토큰을 추출하여 세션을 검증합니다.
     * 세션이 유효하면 사용자 정보를 반환하고, 만료되었으면 401 응답을 반환합니다.
     *
     * @param token 세션 토큰
     * @return 사용자 정보 (userId, username, email, role, assignedBrand)
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> checkSession(
            @RequestHeader(SESSION_TOKEN_HEADER) String token) {

        User user = authenticationService.validateSession(token);

        return ResponseEntity.ok(Map.of(
                "userId", user.getId().toString(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "assignedBrand", user.getAssignedBrand() != null ? user.getAssignedBrand() : ""
        ));
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * 프록시/로드밸런서 뒤에 있는 경우 X-Forwarded-For 헤더를 우선 사용합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can contain multiple IPs; the first is the client
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
