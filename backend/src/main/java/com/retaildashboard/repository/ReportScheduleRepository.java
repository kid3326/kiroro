package com.retaildashboard.repository;

import com.retaildashboard.domain.ReportFrequency;
import com.retaildashboard.domain.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 리포트 스케줄 데이터 접근 Repository.
 *
 * Requirements: 11.1
 */
@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Integer> {

    List<ReportSchedule> findByIsActiveTrue();

    List<ReportSchedule> findByFrequencyAndIsActiveTrue(ReportFrequency frequency);
}
