package com.retaildashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * daily_aggregates 복합 기본키 클래스.
 * 파티셔닝을 위해 (id, aggregate_date) 복합키를 사용합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyAggregateId implements Serializable {
    private Long id;
    private LocalDate aggregateDate;
}
