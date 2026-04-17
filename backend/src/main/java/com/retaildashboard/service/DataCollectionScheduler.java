package com.retaildashboard.service;

import com.retaildashboard.domain.AdvertisingCost;
import com.retaildashboard.domain.Inventory;
import com.retaildashboard.domain.SalesTransaction;
import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import com.retaildashboard.repository.AdvertisingCostRepository;
import com.retaildashboard.repository.InventoryRepository;
import com.retaildashboard.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Scheduler 기반 데이터 수집 스케줄러.
 * 60분 주기로 Mock API에서 데이터를 수집하여 DB에 저장합니다.
 *
 * 수집 흐름:
 * 1. ApiClient를 통해 판매, 광고비, 재고 데이터 수집
 * 2. 수집된 데이터를 각 테이블에 저장
 * 3. 수집 완료 후 Data Aggregator 트리거 (향후 구현)
 * 4. 에러 발생 시 로깅 후 계속 진행
 *
 * Requirements: 1.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataCollectionScheduler {

    private final ApiClient apiClient;
    private final SalesTransactionRepository salesTransactionRepository;
    private final AdvertisingCostRepository advertisingCostRepository;
    private final InventoryRepository inventoryRepository;
    private final AggregationBatchService aggregationBatchService;

    /**
     * 60분 주기 데이터 수집 스케줄러.
     * 최근 1시간 동안의 데이터를 수집합니다.
     */
    @Scheduled(fixedRate = 3600000)
    public void collectData() {
        log.info("=== 데이터 수집 시작 ===");
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(1);

        boolean salesSuccess = collectSalesData(from, to);
        boolean costsSuccess = collectAdvertisingCosts(from, to);
        boolean inventorySuccess = collectInventoryData();

        if (salesSuccess || costsSuccess || inventorySuccess) {
            triggerAggregation();
        }

        log.info("=== 데이터 수집 완료 === 판매: {}, 광고비: {}, 재고: {}",
                salesSuccess ? "성공" : "실패",
                costsSuccess ? "성공" : "실패",
                inventorySuccess ? "성공" : "실패");
    }

    /**
     * 판매 데이터를 수집하여 sales_transactions 테이블에 저장합니다.
     *
     * @param from 시작 일시
     * @param to   종료 일시
     * @return 수집 성공 여부
     */
    @Transactional
    public boolean collectSalesData(LocalDateTime from, LocalDateTime to) {
        try {
            SalesDataResponse response = apiClient.fetchSalesData(from, to);
            if (response == null) {
                log.warn("판매 데이터 수집 실패: API 응답이 null (캐시 데이터 사용)");
                return false;
            }

            List<SalesTransaction> entities = new ArrayList<>();
            for (SalesDataResponse.SalesTransaction tx : response.transactions()) {
                SalesTransaction entity = SalesTransaction.builder()
                        .sku(tx.sku())
                        .transactionTime(tx.transactionTime())
                        .quantity(tx.quantity())
                        .unitPrice(tx.unitPrice())
                        .totalAmount(tx.totalAmount())
                        .discountAmount(tx.discountAmount())
                        .returnAmount(tx.returnAmount())
                        .channel(tx.channel())
                        .isBundle(tx.isBundle())
                        .bundleId(tx.bundleId())
                        .build();
                entities.add(entity);
            }

            salesTransactionRepository.saveAll(entities);
            log.info("판매 데이터 저장 완료: {}건", entities.size());
            return true;
        } catch (Exception e) {
            log.error("판매 데이터 수집/저장 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 광고비 데이터를 수집하여 advertising_costs 테이블에 저장합니다.
     *
     * @param from 시작 일시
     * @param to   종료 일시
     * @return 수집 성공 여부
     */
    @Transactional
    public boolean collectAdvertisingCosts(LocalDateTime from, LocalDateTime to) {
        try {
            AdvertisingCostResponse response = apiClient.fetchAdvertisingCosts(from, to);
            if (response == null) {
                log.warn("광고비 데이터 수집 실패: API 응답이 null (캐시 데이터 사용)");
                return false;
            }

            List<AdvertisingCost> entities = new ArrayList<>();
            for (AdvertisingCostResponse.AdvertisingCost cost : response.costs()) {
                AdvertisingCost entity = AdvertisingCost.builder()
                        .channel(cost.channel())
                        .costDate(cost.costDate())
                        .spendAmount(cost.spendAmount())
                        .impressions(cost.impressions())
                        .clicks(cost.clicks())
                        .conversions(cost.conversions())
                        .newCustomers(cost.newCustomers())
                        .build();
                entities.add(entity);
            }

            advertisingCostRepository.saveAll(entities);
            log.info("광고비 데이터 저장 완료: {}건", entities.size());
            return true;
        } catch (Exception e) {
            log.error("광고비 데이터 수집/저장 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 재고 데이터를 수집하여 inventory 테이블에 저장합니다.
     * 기존 SKU 데이터가 있으면 업데이트, 없으면 새로 생성합니다.
     *
     * @return 수집 성공 여부
     */
    @Transactional
    public boolean collectInventoryData() {
        try {
            InventoryResponse response = apiClient.fetchInventory();
            if (response == null) {
                log.warn("재고 데이터 수집 실패: API 응답이 null (캐시 데이터 사용)");
                return false;
            }

            for (InventoryResponse.InventoryItem item : response.items()) {
                Inventory entity = inventoryRepository.findBySku(item.sku())
                        .map(existing -> {
                            existing.setCurrentQuantity(item.currentQuantity());
                            existing.setReorderPoint(item.reorderPoint());
                            existing.setUnitCost(item.unitCost());
                            existing.setValuationMethod(item.valuationMethod());
                            existing.setLastUpdated(item.lastUpdated());
                            return existing;
                        })
                        .orElseGet(() -> Inventory.builder()
                                .sku(item.sku())
                                .currentQuantity(item.currentQuantity())
                                .reorderPoint(item.reorderPoint())
                                .unitCost(item.unitCost())
                                .valuationMethod(item.valuationMethod())
                                .lastUpdated(item.lastUpdated())
                                .build());

                inventoryRepository.save(entity);
            }

            log.info("재고 데이터 저장 완료: {}건", response.items().size());
            return true;
        } catch (Exception e) {
            log.error("재고 데이터 수집/저장 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 데이터 수집 완료 후 Data Aggregator를 트리거합니다.
     * 일별/월별 집계를 수행하고 캐시를 갱신합니다.
     *
     * Requirements: 8.5
     */
    private void triggerAggregation() {
        log.info("데이터 집계 트리거");
        aggregationBatchService.triggerAggregation();
    }
}
