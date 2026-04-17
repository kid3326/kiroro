package com.retaildashboard.controller;

import com.retaildashboard.config.Audited;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.ExportRequest;
import com.retaildashboard.dto.ExportResponse;
import com.retaildashboard.service.ExportService;
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

import java.util.UUID;

/**
 * 내보내기 REST API 컨트롤러.
 * Excel, PDF, PPT 형식으로 데이터를 내보내고 다운로드 링크를 제공합니다.
 *
 * - POST /api/v1/export/excel - Excel 내보내기
 * - POST /api/v1/export/pdf - PDF 내보내기
 * - POST /api/v1/export/ppt - PPT 내보내기
 * - GET /api/v1/export/{id}/download - 파일 다운로드
 *
 * Requirements: 10.7
 */
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;

    /**
     * Excel 형식으로 내보냅니다.
     * POST /api/v1/export/excel
     *
     * @param request        내보내기 요청
     * @param authentication 인증 정보
     * @return 내보내기 응답 (다운로드 URL 포함)
     */
    @PostMapping("/excel")
    @Audited(eventType = "EXPORT", dataType = "SALES", dataScope = "export_excel")
    public ResponseEntity<ExportResponse> exportExcel(
            @RequestBody ExportRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ExportResponse response = exportService.exportToExcel(request, user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * PDF 형식으로 내보냅니다.
     * POST /api/v1/export/pdf
     *
     * @param request        내보내기 요청
     * @param authentication 인증 정보
     * @return 내보내기 응답 (다운로드 URL 포함)
     */
    @PostMapping("/pdf")
    @Audited(eventType = "EXPORT", dataType = "SALES", dataScope = "export_pdf")
    public ResponseEntity<ExportResponse> exportPdf(
            @RequestBody ExportRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ExportResponse response = exportService.exportToPdf(request, user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * PPT 형식으로 내보냅니다.
     * POST /api/v1/export/ppt
     *
     * @param request        내보내기 요청
     * @param authentication 인증 정보
     * @return 내보내기 응답 (다운로드 URL 포함)
     */
    @PostMapping("/ppt")
    @Audited(eventType = "EXPORT", dataType = "SALES", dataScope = "export_ppt")
    public ResponseEntity<ExportResponse> exportPpt(
            @RequestBody ExportRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ExportResponse response = exportService.exportToPpt(request, user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 내보내기 파일을 다운로드합니다.
     * GET /api/v1/export/{id}/download
     *
     * 24시간 유효한 presigned URL을 반환합니다.
     *
     * @param id             내보내기 작업 ID
     * @param authentication 인증 정보
     * @return 내보내기 응답 (다운로드 URL 포함)
     */
    @GetMapping("/{id}/download")
    @Audited(eventType = "DOWNLOAD", dataType = "SALES", dataScope = "export_download")
    public ResponseEntity<ExportResponse> downloadExport(
            @PathVariable UUID id,
            Authentication authentication) {

        ExportResponse response = exportService.getExportById(id);
        return ResponseEntity.ok(response);
    }
}
