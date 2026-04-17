package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 설정 검증 결과 DTO.
 *
 * Requirements: 17.2, 17.5, 17.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /** 검증 통과 여부 */
    @Builder.Default
    private boolean valid = true;

    /** 검증 오류 목록 */
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    /**
     * 검증 오류를 추가합니다.
     */
    public void addError(int lineNumber, String fieldName, String expectedFormat, String actualValue) {
        errors.add(new ValidationError(lineNumber, fieldName, expectedFormat, actualValue));
        valid = false;
    }

    /**
     * 개별 검증 오류.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        /** 오류 발생 라인 번호 */
        private int lineNumber;
        /** 필드 이름 */
        private String fieldName;
        /** 기대 형식 */
        private String expectedFormat;
        /** 실제 값 */
        private String actualValue;
    }
}
