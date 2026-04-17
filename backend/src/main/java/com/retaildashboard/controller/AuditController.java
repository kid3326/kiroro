package com.retaildashboard.controller;

import com.retaildashboard.config.Audited;
import com.retaildashboard.domain.AuditLog;
import com.retaildashboard.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 감사 로그 REST API 컨트롤러.
 * 컴플라이언스 감사를 위한 감사 로그 조회 및 내보내기 엔드포인트를 제공합니다.
 *
 * Requirements: 15.7
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    /**
     * 감사 로그를 시간 범위로 조회합니다.
     * 선택적으로 사용자 ID로 필터링할 수 있습니다.
     *
     * @param from   시작 시간 (ISO 8601)
     * @param to     종료 시간 (ISO 8601)
     * @param userId 사용자 ID (선택)
     * @param page   페이지 번호 (기본 0)
     * @param size   페이지 크기 (기본 50)
     * @return 감사 로그 페이지
     */
    @GetMapping("/logs")
    @Audited(eventType = "AUDIT_LOG_EXPORT", dataType = "AUDIT", dataScope = "audit_logs_query")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<AuditLog> auditLogs;
        if (userId != null) {
            auditLogs = auditService.getAuditLogsByUser(userId, from, to, pageable);
            log.info("감사 로그 조회: userId={}, from={}, to={}", userId, from, to);
        } else {
            auditLogs = auditService.getAuditLogs(from, to, pageable);
            log.info("감사 로그 조회: from={}, to={}", from, to);
        }

        return ResponseEntity.ok(auditLogs);
    }
}
