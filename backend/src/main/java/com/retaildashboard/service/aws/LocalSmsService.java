package com.retaildashboard.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS 서비스의 로컬 구현체.
 * AWS SNS 대신 로그로 SMS 발송을 기록합니다.
 * 개발/테스트 환경에서 사용됩니다.
 */
@Service
@Slf4j
public class LocalSmsService implements SmsService {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("[SNS Placeholder] SMS 발송: phoneNumber={}, message={}", phoneNumber, message);
    }
}
