package com.retaildashboard.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 이메일 서비스의 로컬 구현체.
 * AWS SES 대신 로그로 이메일 발송을 기록합니다.
 * 개발/테스트 환경에서 사용됩니다.
 */
@Service
@Slf4j
public class LocalEmailService implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[SES Placeholder] 이메일 발송: to={}, subject={}, bodyLength={}",
                to, subject, body.length());
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String body,
                                         String attachmentName, byte[] attachment) {
        log.info("[SES Placeholder] 첨부 이메일 발송: to={}, subject={}, attachment={}, size={} bytes",
                to, subject, attachmentName, attachment != null ? attachment.length : 0);
    }
}
