package com.retaildashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * sales_transactions 복합 기본키 클래스.
 * 파티셔닝을 위해 (id, transaction_time) 복합키를 사용합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransactionId implements Serializable {
    private Long id;
    private LocalDateTime transactionTime;
}
