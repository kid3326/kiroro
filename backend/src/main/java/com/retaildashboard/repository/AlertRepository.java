package com.retaildashboard.repository;

import com.retaildashboard.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 알림 데이터 접근 Repository.
 *
 * Requirements: 12.1-12.6
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserIdAndIsAcknowledgedFalseOrderByTriggeredAtDesc(UUID userId);

    List<Alert> findByUserIdOrderByTriggeredAtDesc(UUID userId);
}
