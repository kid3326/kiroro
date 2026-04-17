package com.retaildashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AuditLog 복합 기본키 클래스.
 * audit_logs 테이블은 월별 파티셔닝을 위해 (id, event_time) 복합 PK를 사용합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogId implements Serializable {

    private Long id;
    private LocalDateTime eventTime;
}
