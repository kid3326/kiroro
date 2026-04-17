package com.retaildashboard.service.aws;

/**
 * S3 파일 저장 서비스 인터페이스.
 * AWS S3에 파일을 업로드하고 presigned URL을 생성합니다.
 */
public interface S3StorageService {

    /**
     * 파일을 S3에 업로드합니다.
     *
     * @param key      S3 객체 키
     * @param content  파일 바이트 배열
     * @param contentType MIME 타입
     * @return 업로드된 파일의 S3 URI
     */
    String uploadFile(String key, byte[] content, String contentType);

    /**
     * 24시간 유효한 presigned URL을 생성합니다.
     *
     * @param key S3 객체 키
     * @return presigned URL 문자열
     */
    String generatePresignedUrl(String key);

    /**
     * S3에서 파일을 다운로드합니다.
     *
     * @param key S3 객체 키
     * @return 파일 바이트 배열
     */
    byte[] downloadFile(String key);
}
