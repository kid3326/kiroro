package com.retaildashboard.controller;

import com.retaildashboard.config.Audited;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.ConfigurationDto;
import com.retaildashboard.dto.ValidationResult;
import com.retaildashboard.service.ConfigurationParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 설정 REST API 컨트롤러.
 * 시스템 설정의 조회, 업데이트, 검증 기능을 제공합니다.
 *
 * - GET /api/v1/config - 현재 설정 조회
 * - PUT /api/v1/config - 설정 업데이트
 * - POST /api/v1/config/validate - 설정 검증
 *
 * Requirements: 17.1, 17.5
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Slf4j
public class ConfigController {

    private final ConfigurationParserService configurationParserService;

    /**
     * 현재 설정을 조회합니다.
     * GET /api/v1/config
     *
     * Requirement 17.1
     *
     * @return 현재 설정
     */
    @GetMapping
    @Audited(eventType = "DATA_ACCESS", dataType = "CONFIG", dataScope = "config_read")
    public ResponseEntity<ConfigurationDto> getConfig() {
        ConfigurationDto config = configurationParserService.getCurrentConfig();
        return ResponseEntity.ok(config);
    }

    /**
     * 설정을 업데이트합니다.
     * PUT /api/v1/config
     *
     * Requirement 17.1
     *
     * @param dto            설정 DTO
     * @param authentication 인증 정보
     * @return 업데이트된 설정
     */
    @PutMapping
    @Audited(eventType = "CONFIG_UPDATE", dataType = "CONFIG", dataScope = "config_update")
    public ResponseEntity<ConfigurationDto> updateConfig(
            @RequestBody ConfigurationDto dto,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ConfigurationDto updated = configurationParserService.updateConfig(dto, user.getId());
        return ResponseEntity.ok(updated);
    }

    /**
     * 설정을 검증합니다.
     * POST /api/v1/config/validate
     *
     * Requirement 17.5
     *
     * @param dto 설정 DTO
     * @return 검증 결과
     */
    @PostMapping("/validate")
    @Audited(eventType = "CONFIG_VALIDATE", dataType = "CONFIG", dataScope = "config_validate")
    public ResponseEntity<ValidationResult> validateConfig(@RequestBody ConfigurationDto dto) {
        ValidationResult result = configurationParserService.validate(dto.getSettings());
        return ResponseEntity.ok(result);
    }
}
