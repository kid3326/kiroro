package com.retaildashboard.service;

import com.retaildashboard.domain.Role;
import com.retaildashboard.domain.Session;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.LoginRequest;
import com.retaildashboard.dto.LoginResponse;
import com.retaildashboard.exception.AccountLockedException;
import com.retaildashboard.exception.AuthenticationException;
import com.retaildashboard.exception.BadRequestException;
import com.retaildashboard.repository.SessionRepository;
import com.retaildashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private LoginRequest loginRequest;

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
                .lockedUntil(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("ValidPass1")
                .ipAddress("127.0.0.1")
                .build();
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTests {

        @Test
        @DisplayName("유효한 자격 증명으로 로그인 성공")
        void login_withValidCredentials_shouldSucceed() {
            // given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("ValidPass1", testUser.getPasswordHash())).thenReturn(true);
            when(sessionRepository.findByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
                Session s = invocation.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            LoginResponse response = authenticationService.login(loginRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getRole()).isEqualTo(Role.FINANCE);
            assertThat(response.getToken()).isNotBlank();
            assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(response.getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인 실패")
        void login_withNonExistentUser_shouldThrowAuthenticationException() {
            // given
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            loginRequest.setUsername("unknown");

            // when & then
            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("잘못된 사용자명 또는 비밀번호");
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패 시 실패 카운트 증가")
        void login_withWrongPassword_shouldIncrementFailCount() {
            // given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPass1", testUser.getPasswordHash())).thenReturn(false);
            loginRequest.setPassword("WrongPass1");

            // when & then
            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(AuthenticationException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("비활성화된 계정으로 로그인 실패")
        void login_withInactiveAccount_shouldThrowAuthenticationException() {
            // given
            testUser.setIsActive(false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("ValidPass1", testUser.getPasswordHash())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("비활성화된 계정");
        }

        @Test
        @DisplayName("로그인 성공 시 세션이 60분 타임아웃으로 생성됨")
        void login_shouldCreateSessionWith60MinTimeout() {
            // given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("ValidPass1", testUser.getPasswordHash())).thenReturn(true);
            when(sessionRepository.findByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
                Session s = invocation.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            LoginResponse response = authenticationService.login(loginRequest);

            // then
            assertThat(response.getExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(59))
                    .isBefore(LocalDateTime.now().plusMinutes(61));
        }
    }

    @Nested
    @DisplayName("동시 로그인 방지 테스트")
    class ConcurrentLoginTests {

        @Test
        @DisplayName("로그인 시 기존 세션이 종료됨")
        void login_shouldTerminateExistingSessions() {
            // given
            Session existingSession = Session.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .token("old-token")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("ValidPass1", testUser.getPasswordHash())).thenReturn(true);
            when(sessionRepository.findByUserId(testUser.getId())).thenReturn(List.of(existingSession));
            when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
                Session s = invocation.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            authenticationService.login(loginRequest);

            // then
            verify(redisTemplate).delete("session:old-token");
            verify(sessionRepository).deleteByUserId(testUser.getId());
        }
    }

    @Nested
    @DisplayName("계정 잠금 테스트")
    class AccountLockTests {

        @Test
        @DisplayName("5회 연속 실패 시 계정 잠금")
        void login_after5FailedAttempts_shouldLockAccount() {
            // given
            testUser.setFailedLoginCount(4); // 이미 4회 실패
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPass1", testUser.getPasswordHash())).thenReturn(false);
            loginRequest.setPassword("WrongPass1");

            // when & then
            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("5회 연속 실패");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFailedLoginCount()).isEqualTo(5);
            assertThat(savedUser.getLockedUntil()).isNotNull();
            assertThat(savedUser.getLockedUntil()).isAfter(LocalDateTime.now().plusMinutes(29));
        }

        @Test
        @DisplayName("잠긴 계정으로 로그인 시도 시 AccountLockedException 발생")
        void login_withLockedAccount_shouldThrowAccountLockedException() {
            // given
            testUser.setLockedUntil(LocalDateTime.now().plusMinutes(20));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("계정이 잠겨 있습니다");
        }

        @Test
        @DisplayName("잠금 시간 경과 후 로그인 성공")
        void login_afterLockExpired_shouldSucceed() {
            // given
            testUser.setLockedUntil(LocalDateTime.now().minusMinutes(1)); // 잠금 만료
            testUser.setFailedLoginCount(5);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("ValidPass1", testUser.getPasswordHash())).thenReturn(true);
            when(sessionRepository.findByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
                Session s = invocation.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            LoginResponse response = authenticationService.login(loginRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("비밀번호 정책 테스트")
    class PasswordPolicyTests {

        @Test
        @DisplayName("유효한 비밀번호 통과")
        void validatePasswordPolicy_withValidPassword_shouldPass() {
            authenticationService.validatePasswordPolicy("ValidPass1");
            // 예외 없이 통과
        }

        @Test
        @DisplayName("8자 미만 비밀번호 거부")
        void validatePasswordPolicy_tooShort_shouldThrow() {
            assertThatThrownBy(() -> authenticationService.validatePasswordPolicy("Short1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("최소 8자");
        }

        @Test
        @DisplayName("대문자 없는 비밀번호 거부")
        void validatePasswordPolicy_noUppercase_shouldThrow() {
            assertThatThrownBy(() -> authenticationService.validatePasswordPolicy("lowercase1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("대문자");
        }

        @Test
        @DisplayName("소문자 없는 비밀번호 거부")
        void validatePasswordPolicy_noLowercase_shouldThrow() {
            assertThatThrownBy(() -> authenticationService.validatePasswordPolicy("UPPERCASE1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("소문자");
        }

        @Test
        @DisplayName("숫자 없는 비밀번호 거부")
        void validatePasswordPolicy_noDigit_shouldThrow() {
            assertThatThrownBy(() -> authenticationService.validatePasswordPolicy("NoDigitHere"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("숫자");
        }

        @Test
        @DisplayName("null 비밀번호 거부")
        void validatePasswordPolicy_null_shouldThrow() {
            assertThatThrownBy(() -> authenticationService.validatePasswordPolicy(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("최소 8자");
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTests {

        @Test
        @DisplayName("유효한 토큰으로 로그아웃 성공")
        void logout_withValidToken_shouldDeleteSession() {
            // given
            Session session = Session.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .token("valid-token")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            when(sessionRepository.findByToken("valid-token")).thenReturn(Optional.of(session));

            // when
            authenticationService.logout("valid-token");

            // then
            verify(sessionRepository).delete(session);
            verify(redisTemplate).delete("session:valid-token");
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 로그아웃 시 무시")
        void logout_withInvalidToken_shouldDoNothing() {
            // given
            when(sessionRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            // when
            authenticationService.logout("invalid-token");

            // then
            verify(sessionRepository, never()).delete(any(Session.class));
        }
    }

    @Nested
    @DisplayName("세션 검증 테스트")
    class SessionValidationTests {

        @Test
        @DisplayName("Redis에서 유효한 세션 검증 성공")
        void validateSession_withValidRedisSession_shouldReturnUser() {
            // given
            Session session = Session.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .token("valid-token")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("session:valid-token")).thenReturn(testUser.getId().toString());
            when(sessionRepository.findByToken("valid-token")).thenReturn(Optional.of(session));

            // when
            User result = authenticationService.validateSession("valid-token");

            // then
            assertThat(result).isEqualTo(testUser);
        }

        @Test
        @DisplayName("만료된 세션 검증 시 예외 발생")
        void validateSession_withExpiredSession_shouldThrow() {
            // given
            Session expiredSession = Session.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .token("expired-token")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("session:expired-token")).thenReturn(testUser.getId().toString());
            when(sessionRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredSession));

            // when & then
            assertThatThrownBy(() -> authenticationService.validateSession("expired-token"))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("세션이 만료되었습니다");
        }

        @Test
        @DisplayName("존재하지 않는 세션 검증 시 예외 발생")
        void validateSession_withNonExistentSession_shouldThrow() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("session:unknown-token")).thenReturn(null);
            when(sessionRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authenticationService.validateSession("unknown-token"))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("유효하지 않은 세션");
        }

        @Test
        @DisplayName("Redis에 없지만 DB에 있는 유효한 세션 검증 성공 및 Redis 복구")
        void validateSession_notInRedisButInDb_shouldRecoverAndReturnUser() {
            // given
            Session session = Session.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .token("db-only-token")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("session:db-only-token")).thenReturn(null);
            when(sessionRepository.findByToken("db-only-token")).thenReturn(Optional.of(session));

            // when
            User result = authenticationService.validateSession("db-only-token");

            // then
            assertThat(result).isEqualTo(testUser);
            verify(valueOperations).set(
                    eq("session:db-only-token"),
                    eq(testUser.getId().toString()),
                    eq(Duration.ofMinutes(60))
            );
        }
    }

    @Nested
    @DisplayName("Redis 세션 저장 테스트")
    class RedisSessionTests {

        @Test
        @DisplayName("로그인 시 Redis에 세션이 60분 TTL로 저장됨")
        void login_shouldStoreSessionInRedisWithTTL() {
            // given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("ValidPass1", testUser.getPasswordHash())).thenReturn(true);
            when(sessionRepository.findByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
                Session s = invocation.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            LoginResponse response = authenticationService.login(loginRequest);

            // then
            verify(valueOperations).set(
                    eq("session:" + response.getToken()),
                    eq(testUser.getId().toString()),
                    eq(Duration.ofMinutes(60))
            );
        }
    }
}
