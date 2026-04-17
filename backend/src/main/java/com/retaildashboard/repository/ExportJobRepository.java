package com.retaildashboard.repository;

import com.retaildashboard.domain.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 내보내기 작업 데이터 접근 Repository.
 *
 * Requirements: 10.7
 */
@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {

    List<ExportJob> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
