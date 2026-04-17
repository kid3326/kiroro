package com.retaildashboard.repository;

import com.retaildashboard.domain.BudgetTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 예산 목표 데이터 접근 Repository.
 *
 * Requirements: 4.13
 */
@Repository
public interface BudgetTargetRepository extends JpaRepository<BudgetTarget, Integer> {

    List<BudgetTarget> findByTargetMonth(LocalDate targetMonth);

    Optional<BudgetTarget> findByTargetMonthAndMetricType(LocalDate targetMonth, String metricType);

    Optional<BudgetTarget> findByTargetMonthAndMetricTypeAndCategory(
            LocalDate targetMonth, String metricType, String category);
}
