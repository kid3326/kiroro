package com.retaildashboard.exception;

/**
 * 인증 실패 예외.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
