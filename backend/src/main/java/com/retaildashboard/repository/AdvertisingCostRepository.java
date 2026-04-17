package com.retaildashboard.repository;

import com.retaildashboard.domain.AdvertisingCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 광고비 데이터 접근 Repository.
 *
 * Requirements: 4.5, 4.6, 4.7
 */
@Repository
public interface AdvertisingCostRepository extends JpaRepository<AdvertisingCost, Long> {

    List<AdvertisingCost> findByCostDateBetween(LocalDate from, LocalDate to);

    List<AdvertisingCost> findByCostDateBetweenAndChannel(LocalDate from, LocalDate to, String channel);

    @Query("SELECT COALESCE(SUM(a.spendAmount), 0) FROM AdvertisingCost a " +
            "WHERE a.costDate BETWEEN :from AND :to")
    BigDecimal sumSpendAmountByPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.spendAmount), 0) FROM AdvertisingCost a " +
            "WHERE a.costDate BETWEEN :from AND :to AND a.channel = :channel")
    BigDecimal sumSpendAmountByPeriodAndChannel(
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("channel") String channel);

    @Query("SELECT COALESCE(SUM(a.newCustomers), 0) FROM AdvertisingCost a " +
            "WHERE a.costDate BETWEEN :from AND :to")
    Long sumNewCustomersByPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
