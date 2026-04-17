package com.retaildashboard.repository;

import com.retaildashboard.domain.AlertConfig;
import com.retaildashboard.domain.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 알림 설정 데이터 접근 Repository.
 *
 * Requirements: 12.7
 */
@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, Integer> {

    List<AlertConfig> findByUserId(UUID userId);

    Optional<AlertConfig> findByUserIdAndAlertType(UUID userId, AlertType alertType);
}
