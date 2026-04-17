package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 차트 스냅샷 DTO.
 * 내보내기 시 차트 이미지를 포함합니다.
 *
 * Requirements: 10.3, 10.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartSnapshot {

    /** 차트 제목 */
    private String title;

    /** Base64 인코딩된 차트 이미지 (최소 300 DPI) */
    private String imageBase64;

    /** 차트 유형 (line, bar, pie) */
    private String chartType;
}
