package com.retaildashboard.dto;

import com.retaildashboard.domain.AlertSeverity;
import com.retaildashboard.domain.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 알림 설정 요청 DTO.
 *
 * Requirements: 12.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigRequest {

    /** 알림 유형 */
    private AlertType alertType;

    /** 임계값 */
    private BigDecimal thresholdValue;

    /** 심각도 */
    private AlertSeverity severity;

    /** 이메일 알림 활성화 */
    private Boolean emailEnabled;

    /** SMS 알림 활성화 */
    private Boolean smsEnabled;

    /** 푸시 알림 활성화 */
    private Boolean pushEnabled;
}
