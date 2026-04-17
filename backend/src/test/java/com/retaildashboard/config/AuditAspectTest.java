package com.retaildashboard.config;

import com.retaildashboard.domain.AccessResult;
import com.retaildashboard.domain.Role;
import com.retaildashboard.domain.User;
import com.retaildashboard.exception.AccessDeniedException;
import com.retaildashboard.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditAspect 단위 테스트.
 * AOP 기반 감사 로깅 인터셉터의 동작을 검증합니다.
 *
 * Requirements: 3.7, 14.7, 15.7
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditAspect auditAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private HttpServletRequest httpRequest;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("test_user")
                .passwordHash("hash")
                .email("test@test.com")
                .role(Role.FINANCE)
                .isActive(true)
                .failedLoginCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void setUpSecurityContext(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setUpRequestContext(String method, String ip) {
        when(httpRequest.getMethod()).thenReturn(method);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn(ip);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));
    }

    private Audited createAuditedAnnotation(String eventType, String dataType, String dataScope) {
        Audited audited = mock(Audited.class);
        when(audited.eventType()).thenReturn(eventType);
        when(audited.dataType()).thenReturn(dataType);
        when(audited.dataScope()).thenReturn(dataScope);
        return audited;
    }

    private void setUpJoinPointMethod(String methodName) throws NoSuchMethodException {
        MethodSignature signature = mock(MethodSignature.class);
        Method method = SampleController.class.getMethod(methodName);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @Test
    @DisplayName("접근 성공 시 GRANTED 감사 로그 기록")
    void auditMethodExecution_onSuccess_shouldLogGranted() throws Throwable {
        setUpSecurityContext(testUser);
        setUpRequestContext("GET", "10.0.0.1");

        Audited audited = createAuditedAnnotation("DATA_ACCESS", "SALES", "dashboard_summary");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = auditAspect.auditMethodExecution(joinPoint, audited);

        assertThat(result).isEqualTo("result");
        verify(auditService).logAccess(
                eq(testUser.getId()),
                eq("DATA_ACCESS"),
                eq("SALES"),
                eq("dashboard_summary"),
                eq("GET"),
                eq(AccessResult.GRANTED),
                eq("10.0.0.1"));
    }

    @Test
    @DisplayName("AccessDeniedException 발생 시 DENIED 감사 로그 기록")
    void auditMethodExecution_onAccessDenied_shouldLogDenied() throws Throwable {
        setUpSecurityContext(testUser);
        setUpRequestContext("GET", "10.0.0.1");

        Audited audited = createAuditedAnnotation("DATA_ACCESS", "COSTS", "cost_data");
        when(joinPoint.proceed()).thenThrow(new AccessDeniedException("접근 거부"));

        assertThatThrownBy(() -> auditAspect.auditMethodExecution(joinPoint, audited))
                .isInstanceOf(AccessDeniedException.class);

        verify(auditService).logAccessSync(
                eq(testUser.getId()),
                eq("DATA_ACCESS"),
                eq("COSTS"),
                eq("cost_data"),
                eq("GET"),
                eq(AccessResult.DENIED),
                eq("10.0.0.1"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 감사 로그를 기록하지 않음")
    void auditMethodExecution_unauthenticated_shouldNotLog() throws Throwable {
        // SecurityContext에 인증 정보 없음
        setUpRequestContext("GET", "10.0.0.1");

        Audited audited = createAuditedAnnotation("DATA_ACCESS", "SALES", "");
        setUpJoinPointMethod("getSalesData");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = auditAspect.auditMethodExecution(joinPoint, audited);

        assertThat(result).isEqualTo("result");
        verify(auditService, never()).logAccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있으면 첫 번째 IP 사용")
    void auditMethodExecution_withXForwardedFor_shouldUseFirstIp() throws Throwable {
        setUpSecurityContext(testUser);

        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        Audited audited = createAuditedAnnotation("DATA_ACCESS", "SALES", "revenue");
        when(joinPoint.proceed()).thenReturn("result");

        auditAspect.auditMethodExecution(joinPoint, audited);

        verify(auditService).logAccess(
                any(), any(), any(), any(), any(), any(),
                eq("203.0.113.50"));
    }

    @Test
    @DisplayName("dataScope가 비어있으면 메서드명 사용")
    void auditMethodExecution_emptyDataScope_shouldUseMethodName() throws Throwable {
        setUpSecurityContext(testUser);
        setUpRequestContext("GET", "10.0.0.1");

        Audited audited = createAuditedAnnotation("DATA_ACCESS", "SALES", "");
        setUpJoinPointMethod("getSalesData");
        when(joinPoint.proceed()).thenReturn("result");

        auditAspect.auditMethodExecution(joinPoint, audited);

        verify(auditService).logAccess(
                any(), any(), any(),
                eq("getSalesData"),
                any(), any(), any());
    }

    @Test
    @DisplayName("기타 예외 발생 시 감사 로그를 기록하지 않고 예외 전파")
    void auditMethodExecution_onOtherException_shouldNotLogAndRethrow() throws Throwable {
        setUpSecurityContext(testUser);
        setUpRequestContext("GET", "10.0.0.1");

        Audited audited = createAuditedAnnotation("DATA_ACCESS", "SALES", "data");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("서버 오류"));

        assertThatThrownBy(() -> auditAspect.auditMethodExecution(joinPoint, audited))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("서버 오류");

        verify(auditService, never()).logAccess(any(), any(), any(), any(), any(), any(), any());
        verify(auditService, never()).logAccessSync(any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * 테스트용 샘플 컨트롤러 (메서드명 추출 테스트에 사용).
     */
    public static class SampleController {
        public String getSalesData() {
            return "sales";
        }
    }
}
