package com.retaildashboard.repository;

import com.retaildashboard.domain.SalesTransaction;
import com.retaildashboard.domain.SalesTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 판매 거래 데이터 접근 Repository.
 *
 * Requirements: 4.1, 4.2, 5.2, 5.4
 */
@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, SalesTransactionId> {

    List<SalesTransaction> findByTransactionTimeBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SalesTransaction s " +
            "WHERE s.transactionTime BETWEEN :from AND :to")
    BigDecimal sumTotalAmountByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SalesTransaction s " +
            "WHERE s.transactionTime BETWEEN :from AND :to AND s.sku = :sku")
    BigDecimal sumTotalAmountByPeriodAndSku(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("sku") String sku);

    @Query("SELECT COALESCE(SUM(s.discountAmount), 0) FROM SalesTransaction s " +
            "WHERE s.transactionTime BETWEEN :from AND :to")
    BigDecimal sumDiscountAmountByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.returnAmount), 0) FROM SalesTransaction s " +
            "WHERE s.transactionTime BETWEEN :from AND :to")
    BigDecimal sumReturnAmountByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SalesTransaction s " +
            "WHERE s.transactionTime BETWEEN :from AND :to AND s.channel = :channel")
    BigDecimal sumTotalAmountByPeriodAndChannel(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("channel") String channel);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM SalesTransaction s " +
            "WHERE s.transactionTime BETWEEN :from AND :to AND s.sku = :sku")
    Long sumQuantityByPeriodAndSku(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("sku") String sku);

    List<SalesTransaction> findByTransactionTimeBetweenAndSku(
            LocalDateTime from, LocalDateTime to, String sku);

    List<SalesTransaction> findByTransactionTimeBetweenAndIsBundle(
            LocalDateTime from, LocalDateTime to, Boolean isBundle);

    @Query("SELECT DISTINCT s.sku FROM SalesTransaction s " +
            "WHERE s.transactionTime BETWEEN :from AND :to")
    List<String> findDistinctSkusByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
