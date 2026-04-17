package com.retaildashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일별 집계 엔티티.
 * daily_aggregates 파티션 테이블과 매핑됩니다.
 * 복합 기본키(id, aggregate_date)를 사용합니다.
 *
 * Requirements: 8.5, 14.2
 */
@Entity
@Table(name = "daily_aggregates")
@IdClass(DailyAggregateId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @Column(name = "aggregate_date", nullable = false)
    private LocalDate aggregateDate;

    @Column(name = "sku", length = 50)
    private String sku;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "brand", length = 200)
    private String brand;

    @Column(name = "total_revenue", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "net_revenue", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netRevenue = BigDecimal.ZERO;

    @Column(name = "cogs", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cogs = BigDecimal.ZERO;

    @Column(name = "gross_profit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal grossProfit = BigDecimal.ZERO;

    @Column(name = "sales_volume", nullable = false)
    @Builder.Default
    private Integer salesVolume = 0;

    @Column(name = "ad_spend", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal adSpend = BigDecimal.ZERO;

    @Column(name = "roas", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal roas = BigDecimal.ZERO;

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
