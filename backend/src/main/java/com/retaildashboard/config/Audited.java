package com.retaildashboard.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 로깅 대상 메서드를 표시하는 커스텀 어노테이션.
 * 이 어노테이션이 적용된 컨트롤러 메서드는 AuditAspect에 의해
 * 자동으로 감사 로그가 기록됩니다.
 *
 * 사용 예:
 * <pre>
 * {@code @Audited(eventType = "DATA_ACCESS", dataType = "SALES")}
 * public ResponseEntity<?> getSalesData() { ... }
 * </pre>
 *
 * Requirements: 3.7, 14.7
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * 이벤트 유형 (예: "DATA_ACCESS", "DATA_EXPORT", "FILTER_SAVE").
     */
    String eventType() default "DATA_ACCESS";

    /**
     * 데이터 유형 (예: "SALES", "COSTS", "ADVERTISING", "INVENTORY", "PRODUCT").
     * 비어 있으면 AOP에서 요청 파라미터로부터 추론합니다.
     */
    String dataType() default "";

    /**
     * 데이터 범위 설명 (예: "dashboard_summary", "revenue_timeseries").
     * 비어 있으면 메서드명을 사용합니다.
     */
    String dataScope() default "";
}
