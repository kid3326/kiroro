package com.retaildashboard.service;

import com.retaildashboard.domain.AccessResult;
import com.retaildashboard.domain.AuditLog;
import com.retaildashboard.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditService 단위 테스트.
 * 감사 로그 기록 및 조회 기능을 검증합니다.
 *
 * Requirements: 3.7, 14.7, 15.7
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private final UUID testUserId = UUID.randomUUID();
    private final String testIp = "192.168.1.100";

    @Nested
    @DisplayName("감사 로그 기록 테스트")
    class LogAccessTests {

        @Test
        @DisplayName("GRANTED 접근 로그를 올바르게 기록")
        void logAccess_granted_shouldSaveAuditLog() {
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

            auditService.logAccessSync(testUserId, "DATA_ACCESS", "SALES",
                    "dashboard_summary", "GET", AccessResult.GRANTED, testIp);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(testUserId);
            assertThat(saved.getEventType()).isEqualTo("DATA_ACCESS");
            assertThat(saved.getDataType()).isEqualTo("SALES");
            assertThat(saved.getDataScope()).isEqualTo("dashboard_summary");
            assertThat(saved.getQueryType()).isEqualTo("GET");
            assertThat(saved.getAccessResult()).isEqualTo(AccessResult.GRANTED);
            assertThat(saved.getIpAddress()).isEqualTo(testIp);
            assertThat(saved.getEventTime()).isNotNull();
        }

        @Test
        @DisplayName("DENIED 접근 로그를 올바르게 기록")
        void logAccess_denied_shouldSaveAuditLog() {
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

            auditService.logAccessSync(testUserId, "DATA_ACCESS", "COSTS",
                    "cost_data", "GET", AccessResult.DENIED, testIp);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getAccessResult()).isEqualTo(AccessResult.DENIED);
            assertThat(saved.getDataType()).isEqualTo("COSTS");
        }
    }

    @Nested
    @DisplayName("감사 로그 조회 테스트")
    class QueryAuditLogsTests {

        @Test
        @DisplayName("사용자별 감사 로그 조회")
        void getAuditLogsByUser_shouldReturnLogs() {
            LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2025, 1, 31, 23, 59);

            AuditLog log1 = AuditLog.builder()
                    .id(1L)
                    .userId(testUserId)
                    .eventTime(LocalDateTime.of(2025, 1, 15, 10, 0))
                    .eventType("DATA_ACCESS")
                    .dataType("SALES")
                    .accessResult(AccessResult.GRANTED)
                    .build();

            when(auditLogRepository.findByUserIdAndEventTimeBetween(testUserId, from, to))
                    .thenReturn(List.of(log1));

            List<AuditLog> result = auditService.getAuditLogsByUser(testUserId, from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(testUserId);
        }

        @Test
        @DisplayName("시간 범위별 감사 로그 페이지네이션 조회")
        void getAuditLogs_shouldReturnPagedLogs() {
            LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2025, 1, 31, 23, 59);
            Pageable pageable = PageRequest.of(0, 50);

            AuditLog log1 = AuditLog.builder()
                    .id(1L)
                    .userId(testUserId)
                    .eventTime(LocalDateTime.of(2025, 1, 15, 10, 0))
                    .eventType("DATA_ACCESS")
                    .accessResult(AccessResult.GRANTED)
                    .build();

            Page<AuditLog> page = new PageImpl<>(List.of(log1), pageable, 1);
            when(auditLogRepository.findByEventTimeBetween(from, to, pageable)).thenReturn(page);

            Page<AuditLog> result = auditService.getAuditLogs(from, to, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("사용자별 감사 로그 페이지네이션 조회")
        void getAuditLogsByUser_paged_shouldReturnPagedLogs() {
            LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2025, 1, 31, 23, 59);
            Pageable pageable = PageRequest.of(0, 50);

            Page<AuditLog> page = new PageImpl<>(List.of(), pageable, 0);
            when(auditLogRepository.findByUserIdAndEventTimeBetween(testUserId, from, to, pageable))
                    .thenReturn(page);

            Page<AuditLog> result = auditService.getAuditLogsByUser(testUserId, from, to, pageable);

            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }
}
