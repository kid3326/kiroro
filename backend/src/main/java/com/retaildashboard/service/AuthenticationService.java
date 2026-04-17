package com.retaildashboard.service;

import com.retaildashboard.domain.Session;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.LoginRequest;
import com.retaildashboard.dto.LoginResponse;
import com.retaildashboard.exception.AccountLockedException;
import com.retaildashboard.exception.AuthenticationException;
import com.retaildashboard.exception.BadRequestException;
import com.retaildashboard.repository.SessionRepository;
import com.retaildashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 인증 서비스.
 * 로그인, 로그아웃, 세션 관리, 비밀번호 정책, 계정 잠금을 처리합니다.
 *
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 15.3, 15.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private static final int BCRYPT_STRENGTH = 12;
    private static final int SESSION_TIMEOUT_MINUTES = 60;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int PASSWORD_MIN_LENGTH = 8;

    /**
     * 비밀번호 정책 패턴: 최소 8자, 대문자 1개 이상, 소문자 1개 이상, 숫자 1개 이상.
     */
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASSWORD_LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile(".*\\d.*");

    private static final String REDIS_SESSION_PREFIX = "session:";

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 사용자 로그인을 처리합니다.
     *
     * 1. 사용자 존재 여부 확인
     * 2. 계정 잠금 상태 확인
     * 3. 비밀번호 검증 (bcrypt cost=12)
     * 4. 기존 세션 종료 (동시 로그인 방지)
     * 5. 새 세션 생성 (60분 타임아웃)
     * 6. Redis에 세션 저장
     *
     * @param request 로그인 요청 (username, password, ipAddress)
     * @return 로그인 응답 (token, 사용자 정보, 만료 시간)
     * @throws AuthenticationException 자격 증명이 유효하지 않은 경우
     * @throws AccountLockedException 계정이 잠긴 경우
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("잘못된 사용자명 또는 비밀번호입니다"));

        checkAccountLock(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthenticationException("잘못된 사용자명 또는 비밀번호입니다");
        }

        if (!user.getIsActive()) {
            throw new AuthenticationException("비활성화된 계정입니다");
        }

        // 로그인 성공: 실패 카운트 초기화
        resetFailedLoginCount(user);

        // 동시 로그인 방지: 기존 세션 종료
        terminateExistingSessions(user.getId());

        // 새 세션 생성
        Session session = createSession(user, request.getIpAddress());

        // Redis에 세션 저장
        storeSessionInRedis(session.getToken(), user.getId().toString());

        log.info("사용자 로그인 성공: username={}, sessionToken={}", user.getUsername(), session.getToken());

        return LoginResponse.builder()
                .token(session.getToken())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .assignedBrand(user.getAssignedBrand())
                .expiresAt(session.getExpiresAt())
                .build();
    }

    /**
     * 사용자 로그아웃을 처리합니다.
     * PostgreSQL과 Redis에서 세션을 삭제합니다.
     *
     * @param token 세션 토큰
     */
    @Transactional
    public void logout(String token) {
        Optional<Session> sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            sessionRepository.delete(session);
            removeSessionFromRedis(token);
            log.info("사용자 로그아웃: userId={}", session.getUser().getId());
        }
    }

    /**
     * 세션 유효성을 검증합니다.
     * Redis에서 먼저 확인하고, 없으면 PostgreSQL에서 확인합니다.
     *
     * @param token 세션 토큰
     * @return 세션에 연결된 사용자
     * @throws AuthenticationException 세션이 유효하지 않거나 만료된 경우
     */
    @Transactional
    public User validateSession(String token) {
        // Redis에서 빠른 조회
        String userId = redisTemplate.opsForValue().get(REDIS_SESSION_PREFIX + token);
        if (userId != null) {
            // Redis에 있으면 DB에서 세션 상세 확인
            Session session = sessionRepository.findByToken(token)
                    .orElseThrow(() -> new AuthenticationException("유효하지 않은 세션입니다"));

            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                invalidateSession(session);
                throw new AuthenticationException("세션이 만료되었습니다");
            }

            return session.getUser();
        }

        // Redis에 없으면 DB에서 직접 확인
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException("유효하지 않은 세션입니다"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            invalidateSession(session);
            throw new AuthenticationException("세션이 만료되었습니다");
        }

        // Redis에 다시 저장 (캐시 복구)
        storeSessionInRedis(token, session.getUser().getId().toString());

        return session.getUser();
    }

    /**
     * 비밀번호 정책을 검증합니다.
     * - 최소 8자 이상
     * - 대문자 1개 이상
     * - 소문자 1개 이상
     * - 숫자 1개 이상
     *
     * @param password 검증할 비밀번호
     * @throws BadRequestException 비밀번호 정책을 충족하지 않는 경우
     */
    public void validatePasswordPolicy(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            throw new BadRequestException("비밀번호는 최소 " + PASSWORD_MIN_LENGTH + "자 이상이어야 합니다");
        }
        if (!PASSWORD_UPPERCASE.matcher(password).matches()) {
            throw new BadRequestException("비밀번호에 대문자가 최소 1개 포함되어야 합니다");
        }
        if (!PASSWORD_LOWERCASE.matcher(password).matches()) {
            throw new BadRequestException("비밀번호에 소문자가 최소 1개 포함되어야 합니다");
        }
        if (!PASSWORD_DIGIT.matcher(password).matches()) {
            throw new BadRequestException("비밀번호에 숫자가 최소 1개 포함되어야 합니다");
        }
    }

    /**
     * 비밀번호를 bcrypt(cost=12)로 인코딩합니다.
     *
     * @param rawPassword 원본 비밀번호
     * @return bcrypt 해시
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 만료된 세션을 정리합니다.
     * 스케줄러에서 주기적으로 호출됩니다.
     */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.deleteByExpiresAtBefore(now);
        log.info("만료된 세션 정리 완료: before={}", now);
    }

    // ========== Private Methods ==========

    /**
     * 계정 잠금 상태를 확인합니다.
     */
    private void checkAccountLock(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException(
                    "계정이 잠겨 있습니다. " + user.getLockedUntil() + "까지 대기해주세요",
                    user.getLockedUntil()
            );
        }

        // 잠금 시간이 지났으면 잠금 해제
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setLockedUntil(null);
            user.setFailedLoginCount(0);
            userRepository.save(user);
        }
    }

    /**
     * 로그인 실패를 처리합니다.
     * 5회 연속 실패 시 30분 잠금.
     */
    private void handleFailedLogin(User user) {
        int newFailCount = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(newFailCount);
        user.setUpdatedAt(LocalDateTime.now());

        if (newFailCount >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
            user.setLockedUntil(lockUntil);
            userRepository.save(user);
            log.warn("계정 잠금: username={}, failedAttempts={}, lockedUntil={}",
                    user.getUsername(), newFailCount, lockUntil);
            throw new AccountLockedException(
                    "로그인 " + MAX_FAILED_ATTEMPTS + "회 연속 실패로 계정이 잠겼습니다. "
                            + LOCKOUT_DURATION_MINUTES + "분 후 다시 시도해주세요",
                    lockUntil
            );
        }

        userRepository.save(user);
        log.warn("로그인 실패: username={}, failedAttempts={}", user.getUsername(), newFailCount);
    }

    /**
     * 로그인 성공 시 실패 카운트를 초기화합니다.
     */
    private void resetFailedLoginCount(User user) {
        if (user.getFailedLoginCount() > 0) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * 기존 세션을 모두 종료합니다 (동시 로그인 방지).
     */
    private void terminateExistingSessions(UUID userId) {
        var existingSessions = sessionRepository.findByUserId(userId);
        for (Session session : existingSessions) {
            removeSessionFromRedis(session.getToken());
        }
        sessionRepository.deleteByUserId(userId);
        log.debug("기존 세션 종료: userId={}, count={}", userId, existingSessions.size());
    }

    /**
     * 새 세션을 생성합니다.
     */
    private Session createSession(User user, String ipAddress) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES);

        Session session = Session.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Redis에 세션을 저장합니다.
     */
    private void storeSessionInRedis(String token, String userId) {
        redisTemplate.opsForValue().set(
                REDIS_SESSION_PREFIX + token,
                userId,
                Duration.ofMinutes(SESSION_TIMEOUT_MINUTES)
        );
    }

    /**
     * Redis에서 세션을 삭제합니다.
     */
    private void removeSessionFromRedis(String token) {
        redisTemplate.delete(REDIS_SESSION_PREFIX + token);
    }

    /**
     * 세션을 무효화합니다 (DB + Redis).
     */
    private void invalidateSession(Session session) {
        removeSessionFromRedis(session.getToken());
        sessionRepository.delete(session);
    }
}
