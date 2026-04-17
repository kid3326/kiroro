package com.retaildashboard.service.aws;

/**
 * 이메일 발송 서비스 인터페이스.
 * AWS SES를 통해 이메일을 발송합니다.
 */
public interface EmailService {

    /**
     * 텍스트 이메일을 발송합니다.
     *
     * @param to      수신자 이메일
     * @param subject 제목
     * @param body    본문
     */
    void sendEmail(String to, String subject, String body);

    /**
     * PDF 첨부 파일이 포함된 이메일을 발송합니다.
     *
     * @param to             수신자 이메일
     * @param subject        제목
     * @param body           본문
     * @param attachmentName 첨부 파일명
     * @param attachment     첨부 파일 바이트 배열
     */
    void sendEmailWithAttachment(String to, String subject, String body,
                                  String attachmentName, byte[] attachment);
}
