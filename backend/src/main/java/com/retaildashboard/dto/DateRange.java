package com.retaildashboard.dto;

import java.time.LocalDate;

/**
 * 날짜 범위 DTO.
 * 조회 기간을 지정하는 데 사용됩니다.
 *
 * @param startDate 시작 날짜
 * @param endDate   종료 날짜
 */
public record DateRange(
        LocalDate startDate,
        LocalDate endDate
) {
    public DateRange {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작 날짜와 종료 날짜는 필수입니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다.");
        }
    }
}
