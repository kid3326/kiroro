package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 수신자 정보 DTO.
 *
 * Requirements: 11.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientInfo {

    /** 수신자 유형 (email, department, role) */
    private String type;

    /** 수신자 값 (이메일 주소, 부서명, 역할명) */
    private String value;
}
