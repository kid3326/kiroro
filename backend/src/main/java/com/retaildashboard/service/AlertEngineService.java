package com.retaildashboard.service;

import com.retaildashboard.domain.Alert;
import com.retaildashboard.domain.AlertConfig;
import com.retaildashboard.domain.AlertSeverity;
import com.retaildashboard.domain.AlertType;
import com.retaildashboard.domain.BudgetTarget;
import com.retaildashboard.domain.Inventory;
import com.retaildashboard.dto.AlertConfigRequest;
import com.retaildashboard.exception.ResourceNotFoundException;
import com.retaildashboard.repository.AlertConfigRepository;
import com.retaildashboard.repository.AlertRepository;
import com.retaildashboard.repository.BudgetTargetRepository;
import com.retaildashboard.repository.DailyAggregateRepository;
import com.retaildashboard.repository.InventoryRepository;
import com.retaildashboard.service.aws.EmailService;
import com.retaildashboard.service.aws.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 알림 엔진 서비스.
 * 매출, 광고비, 판매량, 재고에 대한 임계값 모니터링 및 이상 감지를 수행합니다.
 *
 * - 매출 임계값: 목표 대비 90% 미만 시 알림
 * - 광고비 예산: 예산 대비 95% 초과 시 알림
 * - 판매량 이상: 7일 이동평균 대비 20% 이탈 시 알림
 * - 재고 부족: 재주문점 이하 시 알림
 * - 심각도: CRITICAL, IMPORTANT, INFORMATIONAL
 *
 * Requirements: 12.1-12.5, 12.8
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEngineService {

    private final AlertRepository alertRepository;
    private final AlertConfigRepository alertConfigRepository;
    private final BudgetTargetRepository budgetTargetRepository;
    private final DailyAggregateRepository dailyAggregateRepository;
    private final InventoryRepository inventoryRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    /** 매출 임계값: 목표 대비 90% 미만 */
    private static final BigDecimal REVENUE_THRESHOLD_RATIO = new BigDecimal("0.90");

    /** 광고비 예산 임계값: 95% 초과 */
    private static final BigDecimal AD_SPEND_THRESHOLD_RATIO = new BigDecimal("0.95");

    /** 판매량 이상 감지: 7일 이동평균 대비 20% 이탈 */
    private static final BigDecimal ANOMALY_DEVIATION_RATIO = new BigDecimal("0.20");

    /** 이동평균 기간 (일) */
    private static final int MOVING_AVERAGE_DAYS = 7;

    /**
     * 매출 임계값을 체크합니다.
     * 현재 매출이 목표의 90% 미만이면 알림을 트리거합니다.
     *
     * Requirement 12.1
     *
     * @param currentRevenue 현재 매출
     * @param target         목표 매출
     */
    @Transactional
    public void checkRevenueThreshold(BigDecimal currentRevenue, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal threshold = target.multiply(REVENUE_THRESHOLD_RATIO);
        if (currentRevenue.compareTo(threshold) < 0) {
            BigDecimal ratio = currentRevenue.divide(target, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            Alert alert = Alert.builder()
                    .severity(AlertSeverity.CRITICAL)
                    .title("매출 목표 미달")
                    .message(String.format(
                            "현재 매출(%s)이 목표(%s)의 %.1f%%로, 90%% 미만입니다.",
                            currentRevenue, target, ratio.doubleValue()))
                    .build();

            saveAndDispatch(alert);
        }
    }

    /**
     * 광고비 예산을 체크합니다.
     * 현재 지출이 예산의 95%를 초과하면 알림을 트리거합니다.
     *
     * Requirement 12.2
     *
     * @param currentSpend 현재 광고비 지출
     * @param budget       예산
     */
    @Transactional
    public void checkAdSpendBudget(BigDecimal currentSpend, BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal threshold = budget.multiply(AD_SPEND_THRESHOLD_RATIO);
        if (currentSpend.compareTo(threshold) > 0) {
            BigDecimal ratio = currentSpend.divide(budget, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            Alert alert = Alert.builder()
                    .severity(AlertSeverity.IMPORTANT)
                    .title("광고비 예산 초과 경고")
                    .message(String.format(
                            "현재 광고비(%s)가 예산(%s)의 %.1f%%로, 95%%를 초과했습니다.",
                            currentSpend, budget, ratio.doubleValue()))
                    .build();

            saveAndDispatch(alert);
        }
    }

    /**
     * 판매량 이상을 감지합니다.
     * 7일 이동평균 대비 20% 이상 이탈하면 알림을 트리거합니다.
     *
     * Requirement 12.3
     *
     * @param sku           SKU
     * @param recentVolumes 최근 판매량 목록 (최소 7일)
     */
    @Transactional
    public void detectSalesAnomaly(String sku, List<BigDecimal> recentVolumes) {
        if (recentVolumes == null || recentVolumes.size() < MOVING_AVERAGE_DAYS) {
            return;
        }

        // 7일 이동평균 계산
        BigDecimal sum = BigDecimal.ZERO;
        int startIdx = recentVolumes.size() - MOVING_AVERAGE_DAYS - 1;
        if (startIdx < 0) startIdx = 0;
        int count = 0;
        for (int i = startIdx; i < recentVolumes.size() - 1; i++) {
            sum = sum.add(recentVolumes.get(i));
            count++;
        }

        if (count == 0) return;

        BigDecimal movingAverage = sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        if (movingAverage.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal latestVolume = recentVolumes.get(recentVolumes.size() - 1);
        BigDecimal deviation = latestVolume.subtract(movingAverage).abs()
                .divide(movingAverage, 4, RoundingMode.HALF_UP);

        if (deviation.compareTo(ANOMALY_DEVIATION_RATIO) > 0) {
            AlertSeverity severity = deviation.compareTo(new BigDecimal("0.50")) > 0
                    ? AlertSeverity.CRITICAL : AlertSeverity.IMPORTANT;

            Alert alert = Alert.builder()
                    .severity(severity)
                    .title("판매량 이상 감지: " + sku)
                    .message(String.format(
                            "SKU %s의 최근 판매량(%s)이 7일 이동평균(%s) 대비 %.1f%% 이탈했습니다.",
                            sku, latestVolume, movingAverage,
                            deviation.multiply(new BigDecimal("100")).doubleValue()))
                    .build();

            saveAndDispatch(alert);
        }
    }

    /**
     * 재고 수준을 체크합니다.
     * 현재 재고가 재주문점 이하이면 알림을 트리거합니다.
     *
     * Requirement 12.4
     *
     * @param sku          SKU
     * @param currentStock 현재 재고
     * @param reorderPoint 재주문점
     */
    @Transactional
    public void checkInventoryLevel(String sku, int currentStock, int reorderPoint) {
        if (currentStock <= reorderPoint) {
            AlertSeverity severity = currentStock == 0
                    ? AlertSeverity.CRITICAL : AlertSeverity.IMPORTANT;

            Alert alert = Alert.builder()
                    .severity(severity)
                    .title("재고 부족: " + sku)
                    .message(String.format(
                            "SKU %s의 현재 재고(%d)가 재주문점(%d) 이하입니다.",
                            sku, currentStock, reorderPoint))
                    .build();

            saveAndDispatch(alert);
        }
    }

    /**
     * 사용자의 활성 알림 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 활성 알림 목록
     */
    public List<Alert> getActiveAlerts(UUID userId) {
        return alertRepository.findByUserIdAndIsAcknowledgedFalseOrderByTriggeredAtDesc(userId);
    }

    /**
     * 사용자의 전체 알림 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 전체 알림 목록
     */
    public List<Alert> getAllAlerts(UUID userId) {
        return alertRepository.findByUserIdOrderByTriggeredAtDesc(userId);
    }

    /**
     * 알림을 확인 처리합니다.
     *
     * @param alertId 알림 ID
     */
    @Transactional
    public void acknowledgeAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));

        alert.setIsAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("알림 확인: alertId={}", alertId);
    }

    /**
     * 사용자의 알림 설정을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 알림 설정 목록
     */
    public List<AlertConfig> getAlertConfigs(UUID userId) {
        return alertConfigRepository.findByUserId(userId);
    }

    /**
     * 알림 설정을 업데이트합니다.
     *
     * Requirement 12.7
     *
     * @param userId  사용자 ID
     * @param request 알림 설정 요청
     * @return 업데이트된 알림 설정
     */
    @Transactional
    public AlertConfig updateAlertConfig(UUID userId, AlertConfigRequest request) {
        AlertConfig config = alertConfigRepository
                .findByUserIdAndAlertType(userId, request.getAlertType())
                .orElse(AlertConfig.builder()
                        .userId(userId)
                        .alertType(request.getAlertType())
                        .build());

        if (request.getThresholdValue() != null) {
            config.setThresholdValue(request.getThresholdValue());
        }
        if (request.getSeverity() != null) {
            config.setSeverity(request.getSeverity());
        }
        if (request.getEmailEnabled() != null) {
            config.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getSmsEnabled() != null) {
            config.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getPushEnabled() != null) {
            config.setPushEnabled(request.getPushEnabled());
        }

        AlertConfig saved = alertConfigRepository.save(config);
        log.info("알림 설정 업데이트: userId={}, alertType={}", userId, request.getAlertType());
        return saved;
    }

    /**
     * 기본 임계값을 설정합니다.
     * 초기 배포 시 히스토리 기반 추천값을 적용합니다.
     *
     * Requirement 12.8
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void applyDefaultThresholds(UUID userId) {
        createDefaultConfigIfAbsent(userId, AlertType.REVENUE,
                new BigDecimal("0.90"), AlertSeverity.CRITICAL);
        createDefaultConfigIfAbsent(userId, AlertType.AD_SPEND,
                new BigDecimal("0.95"), AlertSeverity.IMPORTANT);
        createDefaultConfigIfAbsent(userId, AlertType.ANOMALY,
                new BigDecimal("0.20"), AlertSeverity.IMPORTANT);
        createDefaultConfigIfAbsent(userId, AlertType.INVENTORY,
                new BigDecimal("100"), AlertSeverity.IMPORTANT);

        log.info("기본 임계값 설정 완료: userId={}", userId);
    }

    /**
     * 전체 시스템 알림 체크를 수행합니다.
     * 스케줄러에서 주기적으로 호출됩니다.
     */
    @Transactional
    public void runAlertChecks() {
        log.info("알림 체크 시작");

        // 매출 임계값 체크
        checkRevenueThresholds();

        // 광고비 예산 체크
        checkAdSpendBudgets();

        // 재고 부족 체크
        checkInventoryLevels();

        log.info("알림 체크 완료");
    }

    // ---- Private helpers ----

    private void checkRevenueThresholds() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);

        List<BudgetTarget> targets = budgetTargetRepository.findByTargetMonth(firstOfMonth);
        for (BudgetTarget target : targets) {
            if ("REVENUE".equals(target.getMetricType())) {
                BigDecimal currentRevenue = dailyAggregateRepository
                        .sumTotalRevenueByDateRange(firstOfMonth, today);
                if (currentRevenue != null) {
                    checkRevenueThreshold(currentRevenue, target.getTargetValue());
                }
            }
        }
    }

    private void checkAdSpendBudgets() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);

        List<BudgetTarget> targets = budgetTargetRepository.findByTargetMonth(firstOfMonth);
        for (BudgetTarget target : targets) {
            if ("AD_SPEND".equals(target.getMetricType())) {
                BigDecimal currentSpend = dailyAggregateRepository
                        .sumAdSpendByDateRange(firstOfMonth, today);
                if (currentSpend != null) {
                    checkAdSpendBudget(currentSpend, target.getTargetValue());
                }
            }
        }
    }

    private void checkInventoryLevels() {
        List<Inventory> allInventory = inventoryRepository.findAll();
        for (Inventory inv : allInventory) {
            checkInventoryLevel(inv.getSku(), inv.getCurrentQuantity(), inv.getReorderPoint());
        }
    }

    private void saveAndDispatch(Alert alert) {
        Alert saved = alertRepository.save(alert);
        dispatchAlert(saved);
    }

    /**
     * 심각도에 따라 알림을 발송합니다.
     *
     * Requirement 12.6:
     * - CRITICAL: 이메일(SES) + SMS(SNS)
     * - IMPORTANT: 이메일(SES)
     * - INFORMATIONAL: 대시보드 내 알림
     */
    private void dispatchAlert(Alert alert) {
        switch (alert.getSeverity()) {
            case CRITICAL -> {
                emailService.sendEmail(
                        "admin@retaildashboard.com",
                        "[CRITICAL] " + alert.getTitle(),
                        alert.getMessage());
                smsService.sendSms("+821000000000", alert.getTitle() + ": " + alert.getMessage());
                log.warn("CRITICAL 알림 발송: {}", alert.getTitle());
            }
            case IMPORTANT -> {
                emailService.sendEmail(
                        "admin@retaildashboard.com",
                        "[IMPORTANT] " + alert.getTitle(),
                        alert.getMessage());
                log.info("IMPORTANT 알림 발송: {}", alert.getTitle());
            }
            case INFORMATIONAL -> {
                log.info("INFORMATIONAL 알림 (대시보드): {}", alert.getTitle());
            }
        }
    }

    private void createDefaultConfigIfAbsent(UUID userId, AlertType type,
                                              BigDecimal threshold, AlertSeverity severity) {
        if (alertConfigRepository.findByUserIdAndAlertType(userId, type).isEmpty()) {
            AlertConfig config = AlertConfig.builder()
                    .userId(userId)
                    .alertType(type)
                    .thresholdValue(threshold)
                    .severity(severity)
                    .emailEnabled(true)
                    .smsEnabled(severity == AlertSeverity.CRITICAL)
                    .pushEnabled(false)
                    .build();
            alertConfigRepository.save(config);
        }
    }
}
