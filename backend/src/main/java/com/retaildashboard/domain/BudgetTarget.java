package com.retaildashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 예산 목표 엔티티.
 * budget_targets 테이블과 매핑됩니다.
 *
 * Requirements: 4.13
 */
@Entity
@Table(name = "budget_targets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "metric_type", nullable = false, columnDefinition = "budget_metric_type")
    private String metricType;

    @Column(name = "target_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal targetValue = BigDecimal.ZERO;
}
