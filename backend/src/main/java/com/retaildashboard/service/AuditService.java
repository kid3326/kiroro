package com.retaildashboard.service;

import com.retaildashboard.domain.AccessResult;
import com.retaildashboard.domain.AuditLog;
import com.retaildashboard.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 감사 로깅 서비스.
 * 모든 데이터 접근 이벤트를 기록하고, 컴플라이언스 감사를 위한 로그 조회/내보내기를 제공합니다.
 *
 * Requirements: 3.7, 14.7, 15.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 감사 로그를 비동기로 기록합니다.
     * AOP 인터셉터에서 호출되며, 데이터 접근 시 자동으로 기록됩니다.
     *
     * @param userId       사용자 ID
     * @param eventType    이벤트 유형 (예: "DATA_ACCESS", "DATA_EXPORT")
     * @param dataType     데이터 유형 (예: "SALES", "COSTS")
     * @param dataScope    데이터 범위 (예: 필터 조건, 브랜드명)
     * @param queryType    쿼리 유형 (예: "GET", "POST")
     * @param accessResult 접근 결과 (GRANTED 또는 DENIED)
     * @param ipAddress    클라이언트 IP 주소
     */
    @Async
    @Transactional
    public void logAccess(UUID userId, String eventType, String dataType,
                          String dataScope, String queryType,
                          AccessResult accessResult, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .eventTime(LocalDateTime.now())
                    .eventType(eventType)
                    .dataType(dataType)
                    .dataScope(dataScope)
                    .queryType(queryType)
                    .accessResult(accessResult)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);

            log.debug("감사 로그 기록: userId={}, eventType={}, dataType={}, accessResult={}",
                    userId, eventType, dataType, accessResult);
        } catch (Exception e) {
            log.error("감사 로그 기록 실패: userId={}, eventType={}, error={}",
                    userId, eventType, e.getMessage(), e);
        }
    }

    /**
     * 감사 로그를 동기적으로 기록합니다.
     * 접근 거부 등 중요한 이벤트에 사용됩니다.
     *
     * @param userId       사용자 ID
     * @param eventType    이벤트 유형
     * @param dataType     데이터 유형
     * @param dataScope    데이터 범위
     * @param queryType    쿼리 유형
     * @param accessResult 접근 결과
     * @param ipAddress    클라이언트 IP 주소
     */
    @Transactional
    public void logAccessSync(UUID userId, String eventType, String dataType,
                              String dataScope, String queryType,
                              AccessResult accessResult, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .eventTime(LocalDateTime.now())
                .eventType(eventType)
                .dataType(dataType)
                .dataScope(dataScope)
                .queryType(queryType)
                .accessResult(accessResult)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);

        log.debug("감사 로그 기록(동기): userId={}, eventType={}, dataType={}, accessResult={}",
                userId, eventType, dataType, accessResult);
    }

    /**
     * 특정 사용자의 감사 로그를 시간 범위로 조회합니다.
     *
     * @param userId 사용자 ID
     * @param from   시작 시간
     * @param to     종료 시간
     * @return 감사 로그 목록
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUser(UUID userId, LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByUserIdAndEventTimeBetween(userId, from, to);
    }

    /**
     * 감사 로그를 시간 범위로 페이지네이션 조회합니다.
     * 컴플라이언스 감사용 내보내기에 사용됩니다.
     *
     * @param from     시작 시간
     * @param to       종료 시간
     * @param pageable 페이지네이션 정보
     * @return 감사 로그 페이지
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.findByEventTimeBetween(from, to, pageable);
    }

    /**
     * 특정 사용자의 감사 로그를 페이지네이션 조회합니다.
     *
     * @param userId   사용자 ID
     * @param from     시작 시간
     * @param to       종료 시간
     * @param pageable 페이지네이션 정보
     * @return 감사 로그 페이지
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(UUID userId, LocalDateTime from, LocalDateTime to,
                                              Pageable pageable) {
        return auditLogRepository.findByUserIdAndEventTimeBetween(userId, from, to, pageable);
    }
}
