package com.retaildashboard.repository;

import com.retaildashboard.domain.AuditLog;
import com.retaildashboard.domain.AuditLogId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 감사 로그 데이터 접근 Repository.
 * audit_logs 파티션 테이블에 대한 쿼리 메서드를 제공합니다.
 *
 * Requirements: 3.7, 14.7, 15.7
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, AuditLogId> {

    /**
     * 특정 사용자의 감사 로그를 시간 범위로 조회합니다.
     *
     * @param userId 사용자 ID
     * @param from   시작 시간
     * @param to     종료 시간
     * @return 감사 로그 목록
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
            "AND a.eventTime >= :from AND a.eventTime <= :to " +
            "ORDER BY a.eventTime DESC")
    List<AuditLog> findByUserIdAndEventTimeBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 시간 범위로 감사 로그를 페이지네이션 조회합니다.
     *
     * @param from     시작 시간
     * @param to       종료 시간
     * @param pageable 페이지네이션 정보
     * @return 감사 로그 페이지
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventTime >= :from AND a.eventTime <= :to " +
            "ORDER BY a.eventTime DESC")
    Page<AuditLog> findByEventTimeBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    /**
     * 특정 사용자의 감사 로그를 페이지네이션 조회합니다.
     *
     * @param userId   사용자 ID
     * @param from     시작 시간
     * @param to       종료 시간
     * @param pageable 페이지네이션 정보
     * @return 감사 로그 페이지
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
            "AND a.eventTime >= :from AND a.eventTime <= :to " +
            "ORDER BY a.eventTime DESC")
    Page<AuditLog> findByUserIdAndEventTimeBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
