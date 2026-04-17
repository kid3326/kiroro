package com.retaildashboard.controller;

import com.retaildashboard.config.Audited;
import com.retaildashboard.domain.ReportHistory;
import com.retaildashboard.domain.ReportSchedule;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.ReportScheduleRequest;
import com.retaildashboard.dto.ReportTemplateResponse;
import com.retaildashboard.service.ReportSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 리포트 REST API 컨트롤러.
 * 리포트 템플릿 조회, 스케줄 생성, 이력 조회, 다운로드 기능을 제공합니다.
 *
 * - GET /api/v1/reports/templates - 리포트 템플릿 목록
 * - POST /api/v1/reports/schedules - 스케줄 생성
 * - GET /api/v1/reports/history - 리포트 이력 (90일 보관)
 * - GET /api/v1/reports/{id}/download - 리포트 다운로드
 *
 * Requirements: 11.8
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportSchedulerService reportSchedulerService;

    /**
     * 리포트 템플릿 목록을 조회합니다.
     * GET /api/v1/reports/templates
     *
     * Requirement 11.2
     *
     * @return 리포트 템플릿 목록
     */
    @GetMapping("/templates")
    @Audited(eventType = "DATA_ACCESS", dataType = "REPORTS", dataScope = "report_templates")
    public ResponseEntity<List<ReportTemplateResponse>> getTemplates() {
        List<ReportTemplateResponse> templates = reportSchedulerService.getTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * 리포트 스케줄을 생성합니다.
     * POST /api/v1/reports/schedules
     *
     * Requirements: 11.1, 11.3, 11.7
     *
     * @param request        스케줄 생성 요청
     * @param authentication 인증 정보
     * @return 생성된 스케줄
     */
    @PostMapping("/schedules")
    @Audited(eventType = "SCHEDULE_CREATE", dataType = "REPORTS", dataScope = "report_schedule")
    public ResponseEntity<ReportSchedule> createSchedule(
            @RequestBody ReportScheduleRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ReportSchedule schedule = reportSchedulerService.createSchedule(request, user.getId());
        return ResponseEntity.ok(schedule);
    }

    /**
     * 리포트 이력을 조회합니다 (90일 보관).
     * GET /api/v1/reports/history
     *
     * Requirement 11.8
     *
     * @return 리포트 이력 목록
     */
    @GetMapping("/history")
    @Audited(eventType = "DATA_ACCESS", dataType = "REPORTS", dataScope = "report_history")
    public ResponseEntity<List<ReportHistory>> getHistory() {
        List<ReportHistory> history = reportSchedulerService.getReportHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * 리포트를 다운로드합니다.
     * GET /api/v1/reports/{id}/download
     *
     * @param id 리포트 ID
     * @return 리포트 이력 (다운로드 URL 포함)
     */
    @GetMapping("/{id}/download")
    @Audited(eventType = "DOWNLOAD", dataType = "REPORTS", dataScope = "report_download")
    public ResponseEntity<ReportHistory> downloadReport(@PathVariable Long id) {
        ReportHistory report = reportSchedulerService.getReportById(id);
        return ResponseEntity.ok(report);
    }
}
