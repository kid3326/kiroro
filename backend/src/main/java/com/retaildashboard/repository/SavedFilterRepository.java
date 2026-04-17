package com.retaildashboard.repository;

import com.retaildashboard.domain.SavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 저장된 필터 데이터 접근 Repository.
 *
 * Requirements: 7.4, 7.5
 */
@Repository
public interface SavedFilterRepository extends JpaRepository<SavedFilter, Integer> {

    /**
     * 사용자 ID로 저장된 필터 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 저장된 필터 목록
     */
    List<SavedFilter> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
