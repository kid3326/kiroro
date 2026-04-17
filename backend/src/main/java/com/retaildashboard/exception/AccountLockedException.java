package com.retaildashboard.exception;

import java.time.LocalDateTime;

/**
 * 계정 잠금 예외.
 * 5회 연속 로그인 실패 시 30분간 계정이 잠깁니다.
 */
public class AccountLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;

    public AccountLockedException(String message, LocalDateTime lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}
