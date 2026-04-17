package com.retaildashboard.config;

import com.retaildashboard.domain.AccessResult;
import com.retaildashboard.domain.User;
import com.retaildashboard.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * AOP 기반 감사 로깅 인터셉터.
 * {@link Audited} 어노테이션이 적용된 컨트롤러 메서드를 인터셉트하여
 * 데이터 접근 이벤트를 자동으로 감사 로그에 기록합니다.
 *
 * - SecurityContext에서 사용자 정보를 추출합니다.
 * - HttpServletRequest에서 클라이언트 IP를 추출합니다.
 * - 접근 성공(GRANTED)과 접근 거부(DENIED) 모두 기록합니다.
 *
 * Requirements: 3.7, 14.7, 15.7
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    /**
     * @Audited 어노테이션이 적용된 메서드를 인터셉트합니다.
     * 메서드 실행 전후로 감사 로그를 기록합니다.
     *
     * @param joinPoint 조인 포인트
     * @param audited   Audited 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    @Around("@annotation(audited)")
    public Object auditMethodExecution(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        UUID userId = extractUserId();
        String ipAddress = extractClientIp();
        String eventType = audited.eventType();
        String dataType = resolveDataType(audited, joinPoint);
        String dataScope = resolveDataScope(audited, joinPoint);
        String queryType = resolveQueryType();

        try {
            Object result = joinPoint.proceed();

            // 접근 성공 로그
            if (userId != null) {
                auditService.logAccess(userId, eventType, dataType, dataScope,
                        queryType, AccessResult.GRANTED, ipAddress);
            }

            return result;
        } catch (com.retaildashboard.exception.AccessDeniedException e) {
            // 접근 거부 로그
            if (userId != null) {
                auditService.logAccessSync(userId, eventType, dataType, dataScope,
                        queryType, AccessResult.DENIED, ipAddress);
            }
            throw e;
        } catch (Exception e) {
            // 기타 예외는 접근 거부로 기록하지 않고 그대로 전파
            throw e;
        }
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 추출합니다.
     *
     * @return 사용자 ID, 인증되지 않은 경우 null
     */
    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    /**
     * HttpServletRequest에서 클라이언트 IP 주소를 추출합니다.
     * 프록시/로드밸런서 뒤에 있는 경우 X-Forwarded-For 헤더를 우선 사용합니다.
     *
     * @return 클라이언트 IP 주소
     */
    private String extractClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * HTTP 요청 메서드를 추출합니다 (GET, POST, PUT, DELETE).
     *
     * @return HTTP 메서드
     */
    private String resolveQueryType() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "UNKNOWN";
        }
        return attributes.getRequest().getMethod();
    }

    /**
     * 데이터 유형을 결정합니다.
     * 어노테이션에 명시된 값이 있으면 사용하고, 없으면 빈 문자열을 반환합니다.
     *
     * @param audited   Audited 어노테이션
     * @param joinPoint 조인 포인트
     * @return 데이터 유형
     */
    private String resolveDataType(Audited audited, ProceedingJoinPoint joinPoint) {
        if (!audited.dataType().isEmpty()) {
            return audited.dataType();
        }
        return "";
    }

    /**
     * 데이터 범위를 결정합니다.
     * 어노테이션에 명시된 값이 있으면 사용하고, 없으면 메서드명을 사용합니다.
     *
     * @param audited   Audited 어노테이션
     * @param joinPoint 조인 포인트
     * @return 데이터 범위
     */
    private String resolveDataScope(Audited audited, ProceedingJoinPoint joinPoint) {
        if (!audited.dataScope().isEmpty()) {
            return audited.dataScope();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getName();
    }
}
