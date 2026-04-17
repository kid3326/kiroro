package com.retaildashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 리포트 스케줄 엔티티.
 * 자동 리포트 생성 스케줄을 저장합니다.
 *
 * Requirements: 11.1, 11.2, 11.3, 11.7
 */
@Entity
@Table(name = "report_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 10)
    private ReportFrequency frequency;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipients", nullable = false, columnDefinition = "jsonb")
    private String recipients;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
