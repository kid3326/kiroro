package com.retaildashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 감사 로그 엔티티.
 * audit_logs 테이블과 매핑됩니다.
 * 월별 파티셔닝이 적용된 테이블이며, 복합 기본키(id, event_time)를 사용합니다.
 *
 * Requirements: 3.7, 14.7, 15.7
 */
@Entity
@Table(name = "audit_logs")
@IdClass(AuditLogId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @Column(name = "event_time", nullable = false)
    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "data_type", length = 100)
    private String dataType;

    @Column(name = "data_scope", length = 200)
    private String dataScope;

    @Column(name = "query_type", length = 100)
    private String queryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_result", nullable = false, columnDefinition = "access_result")
    private AccessResult accessResult;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
