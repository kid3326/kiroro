package com.retaildashboard.dto;

import com.retaildashboard.domain.ReportFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * 리포트 스케줄 생성 요청 DTO.
 *
 * Requirements: 11.1, 11.3, 11.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportScheduleRequest {

    /** 템플릿 이름 */
    private String templateName;

    /** 빈도 (DAILY, WEEKLY) */
    private ReportFrequency frequency;

    /** 스케줄 시간 */
    private LocalTime scheduledTime;

    /** 수신자 목록 (이메일, 부서, 역할) */
    private List<RecipientInfo> recipients;
}
