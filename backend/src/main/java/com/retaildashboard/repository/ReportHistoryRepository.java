package com.retaildashboard.repository;

import com.retaildashboard.domain.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리포트 이력 데이터 접근 Repository.
 *
 * Requirements: 11.8
 */
@Repository
public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {

    List<ReportHistory> findByGeneratedAtAfterOrderByGeneratedAtDesc(LocalDateTime after);

    List<ReportHistory> findByScheduleIdOrderByGeneratedAtDesc(Integer scheduleId);
}
