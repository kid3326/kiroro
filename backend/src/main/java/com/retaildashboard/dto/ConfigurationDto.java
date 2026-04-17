package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 설정 DTO.
 * 설정 키-값 쌍의 맵을 전달합니다.
 *
 * Requirements: 17.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationDto {

    /** 설정 키-값 맵 */
    private Map<String, String> settings;
}
