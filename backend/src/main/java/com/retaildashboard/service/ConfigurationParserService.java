package com.retaildashboard.service;

import com.retaildashboard.domain.Configuration;
import com.retaildashboard.dto.ConfigurationDto;
import com.retaildashboard.dto.ValidationResult;
import com.retaildashboard.exception.BadRequestException;
import com.retaildashboard.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 설정 파서 서비스.
 * 설정 파일의 파싱, 포맷팅, 검증 기능을 제공합니다.
 *
 * - parse(): 설정 파일 문자열 → Configuration 객체 맵
 * - prettyPrint(): Configuration 맵 → 일관된 들여쓰기/정렬의 설정 파일 문자열
 * - validate(): 필수 필드 검증 (DB 연결 문자열, API 엔드포인트, 인증 설정)
 * - 파싱 에러 시 라인 번호 및 문제 설명 포함 에러 메시지
 *
 * Requirements: 17.1-17.3, 17.5, 17.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationParserService {

    private final ConfigurationRepository configurationRepository;

    /** 필수 설정 키 목록 */
    private static final List<String> REQUIRED_KEYS = List.of(
            "database.url",
            "database.username",
            "database.password",
            "api.endpoint",
            "api.token",
            "auth.secret-key",
            "auth.session-timeout"
    );

    /** 설정 값 형식 패턴 */
    private static final Map<String, Pattern> VALUE_PATTERNS = Map.of(
            "database.url", Pattern.compile("^jdbc:[a-z]+://[\\w./:]+.*$"),
            "api.endpoint", Pattern.compile("^https?://[\\w./:]+.*$"),
            "auth.session-timeout", Pattern.compile("^\\d+$")
    );

    /** 설정 값 형식 설명 */
    private static final Map<String, String> VALUE_FORMAT_DESCRIPTIONS = Map.of(
            "database.url", "JDBC URL (예: jdbc:postgresql://localhost:5432/dbname)",
            "api.endpoint", "HTTP(S) URL (예: https://api.example.com)",
            "auth.session-timeout", "양의 정수 (분 단위, 예: 60)"
    );

    /**
     * 설정 파일 문자열을 파싱하여 키-값 맵으로 변환합니다.
     *
     * Requirement 17.1: 유효한 설정 파일 → Configuration 객체
     * Requirement 17.2: 잘못된 설정 파일 → 라인 번호 + 문제 설명 에러
     *
     * @param configContent 설정 파일 문자열
     * @return 키-값 맵
     * @throws BadRequestException 파싱 에러 시
     */
    public Map<String, String> parse(String configContent) {
        if (configContent == null || configContent.isBlank()) {
            throw new BadRequestException("설정 파일 내용이 비어 있습니다.");
        }

        Map<String, String> result = new LinkedHashMap<>();
        String[] lines = configContent.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;

            // 빈 줄 또는 주석 건너뛰기
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            // key=value 또는 key: value 형식 파싱
            int separatorIdx = findSeparator(line);
            if (separatorIdx < 0) {
                throw new BadRequestException(String.format(
                        "라인 %d: 잘못된 형식입니다. 'key=value' 또는 'key: value' 형식이어야 합니다. 실제: '%s'",
                        lineNumber, line));
            }

            String key = line.substring(0, separatorIdx).trim();
            String value = line.substring(separatorIdx + 1).trim();

            if (key.isEmpty()) {
                throw new BadRequestException(String.format(
                        "라인 %d: 키가 비어 있습니다.", lineNumber));
            }

            result.put(key, value);
        }

        return result;
    }

    /**
     * 키-값 맵을 일관된 형식의 설정 파일 문자열로 변환합니다.
     *
     * Requirement 17.3: Configuration → 일관된 들여쓰기/정렬의 설정 파일 문자열
     *
     * @param settings 키-값 맵
     * @return 포맷된 설정 파일 문자열
     */
    public String prettyPrint(Map<String, String> settings) {
        if (settings == null || settings.isEmpty()) {
            return "";
        }

        // 키를 정렬하여 일관된 순서 보장
        List<String> sortedKeys = settings.keySet().stream().sorted().toList();

        StringBuilder sb = new StringBuilder();
        sb.append("# Retail Dashboard Configuration\n");
        sb.append("# Generated: ").append(LocalDateTime.now()).append("\n\n");

        String currentSection = "";
        for (String key : sortedKeys) {
            // 섹션 구분 (첫 번째 점 기준)
            String section = key.contains(".") ? key.substring(0, key.indexOf('.')) : "";
            if (!section.equals(currentSection)) {
                if (!currentSection.isEmpty()) {
                    sb.append("\n");
                }
                sb.append("# ").append(section).append(" settings\n");
                currentSection = section;
            }

            sb.append(key).append("=").append(settings.get(key)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 설정을 검증합니다.
     * 필수 필드 존재 여부와 값 형식을 검증합니다.
     *
     * Requirements: 17.5, 17.6
     *
     * @param settings 키-값 맵
     * @return 검증 결과
     */
    public ValidationResult validate(Map<String, String> settings) {
        ValidationResult result = new ValidationResult();

        if (settings == null || settings.isEmpty()) {
            result.addError(0, "settings", "non-empty configuration", "empty or null");
            return result;
        }

        // 필수 필드 검증
        for (String requiredKey : REQUIRED_KEYS) {
            if (!settings.containsKey(requiredKey) || settings.get(requiredKey).isBlank()) {
                result.addError(0, requiredKey,
                        "required field must be present and non-empty",
                        settings.getOrDefault(requiredKey, "missing"));
            }
        }

        // 값 형식 검증
        for (Map.Entry<String, Pattern> entry : VALUE_PATTERNS.entrySet()) {
            String key = entry.getKey();
            Pattern pattern = entry.getValue();

            if (settings.containsKey(key) && !settings.get(key).isBlank()) {
                String value = settings.get(key);
                if (!pattern.matcher(value).matches()) {
                    result.addError(0, key,
                            VALUE_FORMAT_DESCRIPTIONS.getOrDefault(key, pattern.pattern()),
                            value);
                }
            }
        }

        return result;
    }

    /**
     * 현재 저장된 모든 설정을 조회합니다.
     *
     * @return 설정 DTO
     */
    public ConfigurationDto getCurrentConfig() {
        List<Configuration> configs = configurationRepository.findAll();
        Map<String, String> settings = new LinkedHashMap<>();
        for (Configuration config : configs) {
            settings.put(config.getKey(), config.getValue());
        }
        return ConfigurationDto.builder().settings(settings).build();
    }

    /**
     * 설정을 업데이트합니다.
     *
     * @param dto    설정 DTO
     * @param userId 업데이트 사용자 ID
     * @return 업데이트된 설정 DTO
     */
    @Transactional
    public ConfigurationDto updateConfig(ConfigurationDto dto, UUID userId) {
        // 먼저 검증
        ValidationResult validationResult = validate(dto.getSettings());
        if (!validationResult.isValid()) {
            throw new BadRequestException("설정 검증 실패: " + validationResult.getErrors().toString());
        }

        for (Map.Entry<String, String> entry : dto.getSettings().entrySet()) {
            Configuration config = configurationRepository.findByKey(entry.getKey())
                    .orElse(Configuration.builder()
                            .key(entry.getKey())
                            .isRequired(REQUIRED_KEYS.contains(entry.getKey()))
                            .build());

            config.setValue(entry.getValue());
            config.setUpdatedAt(LocalDateTime.now());
            config.setUpdatedBy(userId);
            configurationRepository.save(config);
        }

        log.info("설정 업데이트 완료: userId={}, keys={}", userId, dto.getSettings().keySet());
        return getCurrentConfig();
    }

    /**
     * 설정 문자열을 파싱하고 검증합니다.
     *
     * @param configContent 설정 파일 문자열
     * @return 검증 결과
     */
    public ValidationResult parseAndValidate(String configContent) {
        try {
            Map<String, String> parsed = parse(configContent);
            return validate(parsed);
        } catch (BadRequestException e) {
            ValidationResult result = new ValidationResult();
            result.addError(0, "parse", "valid configuration format", e.getMessage());
            return result;
        }
    }

    // ---- Private helpers ----

    /**
     * 구분자(= 또는 :) 위치를 찾습니다.
     */
    private int findSeparator(String line) {
        int equalsIdx = line.indexOf('=');
        int colonIdx = line.indexOf(':');

        if (equalsIdx < 0 && colonIdx < 0) {
            return -1;
        }
        if (equalsIdx < 0) return colonIdx;
        if (colonIdx < 0) return equalsIdx;

        // 더 먼저 나오는 구분자 사용
        return Math.min(equalsIdx, colonIdx);
    }
}
