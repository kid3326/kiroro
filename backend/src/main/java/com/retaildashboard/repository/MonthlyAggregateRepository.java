package com.retaildashboard.repository;

import com.retaildashboard.domain.MonthlyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 월별 집계 데이터 접근 Repository.
 *
 * Requirements: 8.5
 */
@Repository
public interface MonthlyAggregateRepository extends JpaRepository<MonthlyAggregate, Long> {

    List<MonthlyAggregate> findByAggregateMonth(LocalDate aggregateMonth);

    List<MonthlyAggregate> findByAggregateMonthBetween(LocalDate startMonth, LocalDate endMonth);

    List<MonthlyAggregate> findByAggregateMonthAndCategory(LocalDate aggregateMonth, String category);

    @Modifying
    @Query("DELETE FROM MonthlyAggregate m WHERE m.aggregateMonth = :month")
    void deleteByAggregateMonth(@Param("month") LocalDate month);
}
