package com.retaildashboard.controller;

import com.retaildashboard.domain.Role;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.LoginRequest;
import com.retaildashboard.dto.LoginResponse;
import com.retaildashboard.exception.AccountLockedException;
import com.retaildashboard.exception.AuthenticationException;
import com.retaildashboard.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .passwordHash("$2a$12$hashedpassword")
                .email("test@example.com")
                .role(Role.FINANCE)
                .assignedBrand("TestBrand")
                .isActive(true)
                .failedLoginCount(0)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("유효한 자격 증명으로 로그인 성공 시 200 반환")
        void login_withValidCredentials_shouldReturn200() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .username("testuser")
                    .password("ValidPass1")
                    .build();

            LoginResponse expectedResponse = LoginResponse.builder()
                    .token("session-token-123")
                    .userId(testUser.getId())
                    .username("testuser")
                    .email("test@example.com")
                    .role(Role.FINANCE)
                    .assignedBrand("TestBrand")
                    .expiresAt(LocalDateTime.now().plusMinutes(60))
                    .build();

            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
            when(authenticationService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

            // when
            ResponseEntity<LoginResponse> response = authController.login(request, httpServletRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isEqualTo("session-token-123");
            assertThat(response.getBody().getUsername()).isEqualTo("testuser");
            assertThat(response.getBody().getRole()).isEqualTo(Role.FINANCE);
        }

        @Test
        @DisplayName("로그인 시 클라이언트 IP가 요청에 설정됨")
        void login_shouldSetClientIpFromRequest() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .username("testuser")
                    .password("ValidPass1")
                    .build();

            LoginResponse expectedResponse = LoginResponse.builder()
                    .token("token")
                    .userId(testUser.getId())
                    .username("testuser")
                    .email("test@example.com")
                    .role(Role.FINANCE)
                    .expiresAt(LocalDateTime.now().plusMinutes(60))
                    .build();

            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.1");
            when(authenticationService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

            // when
            authController.login(request, httpServletRequest);

            // then
            ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
            verify(authenticationService).login(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있으면 첫 번째 IP를 사용")
        void login_withXForwardedFor_shouldUseFirstIp() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .username("testuser")
                    .password("ValidPass1")
                    .build();

            LoginResponse expectedResponse = LoginResponse.builder()
                    .token("token")
                    .userId(testUser.getId())
                    .username("testuser")
                    .email("test@example.com")
                    .role(Role.FINANCE)
                    .expiresAt(LocalDateTime.now().plusMinutes(60))
                    .build();

            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18, 150.172.238.178");
            when(authenticationService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

            // when
            authController.login(request, httpServletRequest);

            // then
            ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
            verify(authenticationService).login(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("잘못된 자격 증명으로 로그인 시 AuthenticationException 발생")
        void login_withInvalidCredentials_shouldThrowAuthenticationException() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .username("testuser")
                    .password("WrongPass1")
                    .build();

            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(authenticationService.login(any(LoginRequest.class)))
                    .thenThrow(new AuthenticationException("잘못된 사용자명 또는 비밀번호입니다"));

            // when & then
            assertThatThrownBy(() -> authController.login(request, httpServletRequest))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("잘못된 사용자명 또는 비밀번호");
        }

        @Test
        @DisplayName("잠긴 계정으로 로그인 시 AccountLockedException 발생")
        void login_withLockedAccount_shouldThrowAccountLockedException() {
            // given
            LoginRequest request = LoginRequest.builder()
                    .username("testuser")
                    .password("ValidPass1")
                    .build();

            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(30);
            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(authenticationService.login(any(LoginRequest.class)))
                    .thenThrow(new AccountLockedException("계정이 잠겨 있습니다", lockedUntil));

            // when & then
            assertThatThrownBy(() -> authController.login(request, httpServletRequest))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("계정이 잠겨 있습니다");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("유효한 토큰으로 로그아웃 성공 시 200 반환")
        void logout_withValidToken_shouldReturn200() {
            // given
            String token = "valid-session-token";
            doNothing().when(authenticationService).logout(token);

            // when
            ResponseEntity<Map<String, String>> response = authController.logout(token);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("message", "로그아웃되었습니다");
            verify(authenticationService).logout(token);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/session")
    class SessionTests {

        @Test
        @DisplayName("유효한 세션 토큰으로 세션 확인 시 사용자 정보 반환")
        void checkSession_withValidToken_shouldReturnUserInfo() {
            // given
            String token = "valid-session-token";
            when(authenticationService.validateSession(token)).thenReturn(testUser);

            // when
            ResponseEntity<Map<String, Object>> response = authController.checkSession(token);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("userId")).isEqualTo(testUser.getId().toString());
            assertThat(response.getBody().get("username")).isEqualTo("testuser");
            assertThat(response.getBody().get("email")).isEqualTo("test@example.com");
            assertThat(response.getBody().get("role")).isEqualTo("FINANCE");
            assertThat(response.getBody().get("assignedBrand")).isEqualTo("TestBrand");
        }

        @Test
        @DisplayName("assignedBrand가 null인 경우 빈 문자열 반환")
        void checkSession_withNullAssignedBrand_shouldReturnEmptyString() {
            // given
            testUser.setAssignedBrand(null);
            String token = "valid-session-token";
            when(authenticationService.validateSession(token)).thenReturn(testUser);

            // when
            ResponseEntity<Map<String, Object>> response = authController.checkSession(token);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("assignedBrand")).isEqualTo("");
        }

        @Test
        @DisplayName("만료된 세션 토큰으로 세션 확인 시 AuthenticationException 발생")
        void checkSession_withExpiredToken_shouldThrowAuthenticationException() {
            // given
            String token = "expired-session-token";
            when(authenticationService.validateSession(token))
                    .thenThrow(new AuthenticationException("세션이 만료되었습니다"));

            // when & then
            assertThatThrownBy(() -> authController.checkSession(token))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("세션이 만료되었습니다");
        }

        @Test
        @DisplayName("유효하지 않은 세션 토큰으로 세션 확인 시 AuthenticationException 발생")
        void checkSession_withInvalidToken_shouldThrowAuthenticationException() {
            // given
            String token = "invalid-token";
            when(authenticationService.validateSession(token))
                    .thenThrow(new AuthenticationException("유효하지 않은 세션입니다"));

            // when & then
            assertThatThrownBy(() -> authController.checkSession(token))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("유효하지 않은 세션");
        }
    }
}
