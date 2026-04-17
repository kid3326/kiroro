package com.retaildashboard.service;

import com.retaildashboard.domain.AuditLog;
import com.retaildashboard.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 컴플라이언스 서비스.
 * 개인정보 수집 동의 관리, 거래 기록 보관 정책, 데이터 접근 로그 내보내기를 제공합니다.
 *
 * - 개인정보보호법 준수: 수집 동의 화면 및 동의 기록 저장
 * - 전자상거래법 준수: 거래 기록 5년 보관
 * - 컴플라이언스 감사: 데이터 접근 로그 CSV 내보내기
 *
 * Requirements: 15.5, 15.6, 15.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    private final AuditLogRepository auditLogRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TRANSACTION_RETENTION_YEARS = 5;

    // ============================================
    // 개인정보 수집 동의 관리
    // ============================================

    /**
     * 사용자의 개인정보 수집 동의를 기록합니다.
     *
     * @param userId      사용자 ID
     * @param consentType 동의 유형 (예: "DATA_COLLECTION", "MARKETING", "THIRD_PARTY")
     * @param consented   동의 여부
     * @param ipAddress   동의 시 IP 주소
     */
    @Transactional
    public void recordConsent(UUID userId, String consentType, boolean consented, String ipAddress) {
        String sql = "INSERT INTO user_consents (user_id, consent_type, consented, ip_address, consented_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (user_id, consent_type) DO UPDATE SET " +
                "consented = EXCLUDED.consented, ip_address = EXCLUDED.ip_address, consented_at = EXCLUDED.consented_at";

        jdbcTemplate.update(sql, userId, consentType, consented, ipAddress, LocalDateTime.now());

        log.info("개인정보 동의 기록: userId={}, type={}, consented={}", userId, consentType, consented);
    }

    /**
     * 사용자의 동의 상태를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 동의 기록 목록
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserConsents(UUID userId) {
        String sql = "SELECT consent_type, consented, consented_at, ip_address " +
                "FROM user_consents WHERE user_id = ? ORDER BY consented_at DESC";
        return jdbcTemplate.queryForList(sql, userId);
    }

    /**
     * 특정 동의 유형에 대한 사용자의 동의 여부를 확인합니다.
     *
     * @param userId      사용자 ID
     * @param consentType 동의 유형
     * @return 동의 여부 (동의 기록이 없으면 false)
     */
    @Transactional(readOnly = true)
    public boolean hasConsent(UUID userId, String consentType) {
        String sql = "SELECT consented FROM user_consents WHERE user_id = ? AND consent_type = ?";
        List<Boolean> results = jdbcTemplate.queryForList(sql, Boolean.class, userId, consentType);
        return !results.isEmpty() && Boolean.TRUE.equals(results.get(0));
    }

    // ============================================
    // 거래 기록 보관 정책
    // ============================================

    /**
     * 거래 기록 보관 정책 상태를 조회합니다.
     * 전자상거래법에 따라 5년 보관 정책이 적용됩니다.
     *
     * @return 보관 정책 정보
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRetentionPolicyStatus() {
        LocalDate retentionCutoff = LocalDate.now().minusYears(TRANSACTION_RETENTION_YEARS);

        // 전체 거래 건수
        String totalCountSql = "SELECT COUNT(*) FROM sales_transactions";
        Long totalCount = jdbcTemplate.queryForObject(totalCountSql, Long.class);

        // 보관 기간 내 거래 건수
        String activeCountSql = "SELECT COUNT(*) FROM sales_transactions WHERE transaction_time >= ?";
        Long activeCount = jdbcTemplate.queryForObject(activeCountSql, Long.class,
                retentionCutoff.atStartOfDay());

        // 가장 오래된 거래 날짜
        String oldestSql = "SELECT MIN(transaction_time) FROM sales_transactions";
        LocalDateTime oldest = jdbcTemplate.queryForObject(oldestSql, LocalDateTime.class);

        return Map.of(
                "retentionYears", TRANSACTION_RETENTION_YEARS,
                "retentionCutoffDate", retentionCutoff.toString(),
                "totalTransactions", totalCount != null ? totalCount : 0,
                "activeTransactions", activeCount != null ? activeCount : 0,
                "oldestTransactionDate", oldest != null ? oldest.format(DATETIME_FORMAT) : "N/A",
                "policyCompliant", true
        );
    }

    // ============================================
    // 데이터 접근 로그 내보내기
    // ============================================

    /**
     * 감사 로그를 CSV 형식으로 내보냅니다.
     * 컴플라이언스 감사용으로 사용됩니다.
     *
     * @param from 시작 시간
     * @param to   종료 시간
     * @return CSV 바이트 배열
     */
    @Transactional(readOnly = true)
    public byte[] exportAuditLogsCsv(LocalDateTime from, LocalDateTime to) {
        log.info("감사 로그 내보내기: {} ~ {}", from, to);

        Page<AuditLog> logs = auditLogRepository.findByEventTimeBetween(from, to, Pageable.unpaged());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // BOM for Excel UTF-8 compatibility
        writer.print('\uFEFF');

        // CSV 헤더
        writer.println("User ID,Event Time,Event Type,Data Type,Data Scope,Query Type,Access Result,IP Address");

        // CSV 데이터
        for (AuditLog auditLog : logs.getContent()) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    escapeCsv(auditLog.getUserId() != null ? auditLog.getUserId().toString() : ""),
                    escapeCsv(auditLog.getEventTime() != null ? auditLog.getEventTime().format(DATETIME_FORMAT) : ""),
                    escapeCsv(auditLog.getEventType()),
                    escapeCsv(auditLog.getDataType()),
                    escapeCsv(auditLog.getDataScope()),
                    escapeCsv(auditLog.getQueryType()),
                    escapeCsv(auditLog.getAccessResult() != null ? auditLog.getAccessResult().name() : ""),
                    escapeCsv(auditLog.getIpAddress()));
        }

        writer.flush();
        log.info("감사 로그 내보내기 완료: {}건", logs.getTotalElements());
        return baos.toByteArray();
    }

    /**
     * 특정 사용자의 감사 로그를 CSV로 내보냅니다.
     *
     * @param userId 사용자 ID
     * @param from   시작 시간
     * @param to     종료 시간
     * @return CSV 바이트 배열
     */
    @Transactional(readOnly = true)
    public byte[] exportUserAuditLogsCsv(UUID userId, LocalDateTime from, LocalDateTime to) {
        log.info("사용자 감사 로그 내보내기: userId={}, {} ~ {}", userId, from, to);

        Page<AuditLog> logs = auditLogRepository.findByUserIdAndEventTimeBetween(
                userId, from, to, Pageable.unpaged());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        writer.print('\uFEFF');
        writer.println("Event Time,Event Type,Data Type,Data Scope,Query Type,Access Result,IP Address");

        for (AuditLog auditLog : logs.getContent()) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    escapeCsv(auditLog.getEventTime() != null ? auditLog.getEventTime().format(DATETIME_FORMAT) : ""),
                    escapeCsv(auditLog.getEventType()),
                    escapeCsv(auditLog.getDataType()),
                    escapeCsv(auditLog.getDataScope()),
                    escapeCsv(auditLog.getQueryType()),
                    escapeCsv(auditLog.getAccessResult() != null ? auditLog.getAccessResult().name() : ""),
                    escapeCsv(auditLog.getIpAddress()));
        }

        writer.flush();
        log.info("사용자 감사 로그 내보내기 완료: userId={}, {}건", userId, logs.getTotalElements());
        return baos.toByteArray();
    }

    /**
     * CSV 값을 이스케이프합니다.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
