package com.retaildashboard.controller;

import com.retaildashboard.config.Audited;
import com.retaildashboard.domain.Alert;
import com.retaildashboard.domain.AlertConfig;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.AlertConfigRequest;
import com.retaildashboard.service.AlertEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 알림 REST API 컨트롤러.
 * 알림 조회, 설정 변경, 알림 확인 기능을 제공합니다.
 *
 * - GET /api/v1/alerts - 활성 알림 목록
 * - PUT /api/v1/alerts/config - 알림 설정 변경
 * - POST /api/v1/alerts/{id}/acknowledge - 알림 확인
 *
 * Requirements: 12.6, 12.7
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertEngineService alertEngineService;

    /**
     * 활성 알림 목록을 조회합니다.
     * GET /api/v1/alerts
     *
     * @param authentication 인증 정보
     * @return 활성 알림 목록
     */
    @GetMapping
    @Audited(eventType = "DATA_ACCESS", dataType = "ALERTS", dataScope = "alerts_list")
    public ResponseEntity<Map<String, Object>> getAlerts(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Alert> alerts = alertEngineService.getAllAlerts(user.getId());
        List<AlertConfig> configs = alertEngineService.getAlertConfigs(user.getId());

        return ResponseEntity.ok(Map.of(
                "alerts", alerts,
                "configs", configs
        ));
    }

    /**
     * 알림 설정을 변경합니다.
     * PUT /api/v1/alerts/config
     *
     * Requirement 12.7
     *
     * @param request        알림 설정 요청
     * @param authentication 인증 정보
     * @return 업데이트된 알림 설정
     */
    @PutMapping("/config")
    @Audited(eventType = "CONFIG_UPDATE", dataType = "ALERTS", dataScope = "alert_config")
    public ResponseEntity<AlertConfig> updateAlertConfig(
            @RequestBody AlertConfigRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        AlertConfig config = alertEngineService.updateAlertConfig(user.getId(), request);
        return ResponseEntity.ok(config);
    }

    /**
     * 알림을 확인 처리합니다.
     * POST /api/v1/alerts/{id}/acknowledge
     *
     * @param id             알림 ID
     * @param authentication 인증 정보
     * @return 성공 응답
     */
    @PostMapping("/{id}/acknowledge")
    @Audited(eventType = "ALERT_ACK", dataType = "ALERTS", dataScope = "alert_acknowledge")
    public ResponseEntity<Map<String, String>> acknowledgeAlert(
            @PathVariable Long id,
            Authentication authentication) {

        alertEngineService.acknowledgeAlert(id);
        return ResponseEntity.ok(Map.of("message", "알림이 확인되었습니다."));
    }
}
