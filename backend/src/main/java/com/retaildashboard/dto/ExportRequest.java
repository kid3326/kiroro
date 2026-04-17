package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 내보내기 요청 DTO.
 *
 * Requirements: 10.1, 10.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    /** 필터 조건 */
    private FilterCriteria filters;

    /** 차트 스냅샷 (Base64 인코딩 이미지) */
    private List<ChartSnapshot> charts;

    /** 날짜 범위 */
    private DateRange dateRange;
}
