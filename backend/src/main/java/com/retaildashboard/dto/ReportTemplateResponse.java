package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 리포트 템플릿 응답 DTO.
 *
 * Requirements: 11.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTemplateResponse {

    /** 템플릿 이름 */
    private String name;

    /** 템플릿 설명 */
    private String description;

    /** 포함 메트릭 목록 */
    private List<String> includedMetrics;
}
