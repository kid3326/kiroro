package com.retaildashboard.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S3 저장 서비스의 로컬 구현체.
 * AWS 자격 증명 없이 동작하며, 메모리에 파일을 저장합니다.
 * 개발/테스트 환경에서 사용됩니다.
 */
@Service
@Slf4j
public class LocalS3StorageService implements S3StorageService {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String uploadFile(String key, byte[] content, String contentType) {
        storage.put(key, content);
        log.info("[S3 Placeholder] 파일 업로드: key={}, size={} bytes, contentType={}",
                key, content.length, contentType);
        return "s3://retail-dashboard-exports/" + key;
    }

    @Override
    public String generatePresignedUrl(String key) {
        String presignedUrl = "http://localhost:8080/api/v1/export/" + key + "/download?token=" + UUID.randomUUID();
        log.info("[S3 Placeholder] Presigned URL 생성: key={}, url={}", key, presignedUrl);
        return presignedUrl;
    }

    @Override
    public byte[] downloadFile(String key) {
        byte[] content = storage.get(key);
        if (content == null) {
            log.warn("[S3 Placeholder] 파일을 찾을 수 없음: key={}", key);
            return new byte[0];
        }
        log.info("[S3 Placeholder] 파일 다운로드: key={}, size={} bytes", key, content.length);
        return content;
    }
}
