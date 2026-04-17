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
import java.time.LocalDateTime;

/**
 * 판매 거래 엔티티.
 * sales_transactions 파티션 테이블과 매핑됩니다.
 * 복합 기본키(id, transaction_time)를 사용합니다.
 *
 * Requirements: 14.1
 */
@Entity
@Table(name = "sales_transactions")
@IdClass(SalesTransactionId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "return_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal returnAmount = BigDecimal.ZERO;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "is_bundle", nullable = false)
    @Builder.Default
    private Boolean isBundle = false;

    @Column(name = "bundle_id")
    private Integer bundleId;
}
