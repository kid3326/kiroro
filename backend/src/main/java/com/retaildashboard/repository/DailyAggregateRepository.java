package com.retaildashboard.repository;

import com.retaildashboard.domain.DailyAggregate;
import com.retaildashboard.domain.DailyAggregateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 일별 집계 데이터 접근 Repository.
 *
 * Requirements: 8.5
 */
@Repository
public interface DailyAggregateRepository extends JpaRepository<DailyAggregate, DailyAggregateId> {

    List<DailyAggregate> findByAggregateDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyAggregate> findByAggregateDateBetweenAndCategory(
            LocalDate startDate, LocalDate endDate, String category);

    List<DailyAggregate> findByAggregateDateBetweenAndBrand(
            LocalDate startDate, LocalDate endDate, String brand);

    List<DailyAggregate> findByAggregateDateBetweenAndSku(
            LocalDate startDate, LocalDate endDate, String sku);

    @Modifying
    @Query("DELETE FROM DailyAggregate d WHERE d.aggregateDate = :date")
    void deleteByAggregateDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(d.totalRevenue), 0) FROM DailyAggregate d WHERE d.aggregateDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumTotalRevenueByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(d.adSpend), 0) FROM DailyAggregate d WHERE d.aggregateDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumAdSpendByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
