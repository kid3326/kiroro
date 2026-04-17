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
 * 광고비 엔티티.
 * advertising_costs 테이블과 매핑됩니다.
 *
 * Requirements: 14.1
 */
@Entity
@Table(name = "advertising_costs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisingCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel", nullable = false, columnDefinition = "ad_channel")
    private String channel;

    @Column(name = "cost_date", nullable = false)
    private LocalDate costDate;

    @Column(name = "spend_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal spendAmount = BigDecimal.ZERO;

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Integer impressions = 0;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Integer clicks = 0;

    @Column(name = "conversions", nullable = false)
    @Builder.Default
    private Integer conversions = 0;

    @Column(name = "new_customers", nullable = false)
    @Builder.Default
    private Integer newCustomers = 0;
}
