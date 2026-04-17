package com.retaildashboard.service.aws;

/**
 * SMS 발송 서비스 인터페이스.
 * AWS SNS를 통해 SMS를 발송합니다.
 */
public interface SmsService {

    /**
     * SMS를 발송합니다.
     *
     * @param phoneNumber 수신자 전화번호 (E.164 형식)
     * @param message     메시지 내용
     */
    void sendSms(String phoneNumber, String message);
}
