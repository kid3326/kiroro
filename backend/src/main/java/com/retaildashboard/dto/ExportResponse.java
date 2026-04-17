package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내보내기 응답 DTO.
 *
 * Requirements: 10.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponse {

    /** 내보내기 작업 ID */
    private UUID id;

    /** 다운로드 URL (24시간 유효) */
    private String downloadUrl;

    /** 파일 크기 (바이트) */
    private Long fileSizeBytes;

    /** 만료 시간 */
    private LocalDateTime expiresAt;

    /** 상태 */
    private String status;
}
