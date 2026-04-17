package com.retaildashboard.service;

import com.retaildashboard.service.aws.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 데이터 아카이빙 서비스.
 * 2년 이상 데이터를 S3 Glacier로 이동하고, 5년 이상 데이터를 자동 삭제합니다.
 * 월별 파티션을 사전 생성하여 데이터 저장을 준비합니다.
 *
 * Requirements: 14.3, 14.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataArchivingService {

    private final JdbcTemplate jdbcTemplate;
    private final S3StorageService s3StorageService;

    @Value("${archiving.s3.bucket:retail-dashboard-archive}")
    private String archiveBucket;

    @Value("${archiving.archive-after-years:2}")
    private int archiveAfterYears;

    @Value("${archiving.delete-after-years:5}")
    private int deleteAfterYears;

    @Value("${archiving.partition-months-ahead:3}")
    private int partitionMonthsAhead;

    private static final DateTimeFormatter PARTITION_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 매일 새벽 2시에 아카이빙 배치를 실행합니다.
     * 1) 2년 이상 데이터를 S3 Glacier로 이동
     * 2) 5년 이상 데이터를 자동 삭제
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runArchivingBatch() {
        log.info("데이터 아카이빙 배치 시작");

        try {
            archiveOldData();
            deleteExpiredData();
            log.info("데이터 아카이빙 배치 완료");
        } catch (Exception e) {
            log.error("데이터 아카이빙 배치 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 2년 이상 된 데이터를 S3 Glacier로 아카이빙합니다.
     * 판매 거래, 일별 집계, 감사 로그를 대상으로 합니다.
     */
    public void archiveOldData() {
        LocalDate archiveCutoff = LocalDate.now().minusYears(archiveAfterYears);
        LocalDateTime archiveCutoffTime = archiveCutoff.atStartOfDay();

        log.info("아카이빙 기준일: {} ({}년 이전 데이터)", archiveCutoff, archiveAfterYears);

        // 판매 거래 아카이빙
        archiveSalesTransactions(archiveCutoffTime);

        // 일별 집계 아카이빙
        archiveDailyAggregates(archiveCutoff);

        // 감사 로그 아카이빙
        archiveAuditLogs(archiveCutoffTime);
    }

    /**
     * 5년 이상 된 데이터를 자동 삭제합니다.
     * 거래 기록 5년 보관 정책에 따라 만료된 데이터를 제거합니다.
     */
    public void deleteExpiredData() {
        LocalDate deleteCutoff = LocalDate.now().minusYears(deleteAfterYears);
        LocalDateTime deleteCutoffTime = deleteCutoff.atStartOfDay();

        log.info("삭제 기준일: {} ({}년 이전 데이터)", deleteCutoff, deleteAfterYears);

        // 판매 거래 삭제 (파티션 드롭)
        dropExpiredPartitions("sales_transactions", deleteCutoff);

        // 일별 집계 삭제 (파티션 드롭)
        dropExpiredPartitions("daily_aggregates", deleteCutoff);

        // 감사 로그 삭제 (파티션 드롭)
        dropExpiredPartitions("audit_logs", deleteCutoff);

        log.info("만료 데이터 삭제 완료: {} 이전 데이터", deleteCutoff);
    }

    /**
     * 매월 1일 새벽 1시에 향후 파티션을 사전 생성합니다.
     * 향후 3개월분의 월별 파티션을 미리 생성하여 데이터 저장을 준비합니다.
     */
    @Scheduled(cron = "0 0 1 1 * *")
    public void createFuturePartitions() {
        log.info("파티션 사전 생성 시작");

        LocalDate now = LocalDate.now();
        String[] tables = {"sales_transactions", "daily_aggregates", "audit_logs"};

        for (String table : tables) {
            for (int i = 1; i <= partitionMonthsAhead; i++) {
                LocalDate partitionMonth = now.plusMonths(i);
                createMonthlyPartition(table, partitionMonth);
            }
        }

        log.info("파티션 사전 생성 완료: 향후 {}개월분", partitionMonthsAhead);
    }

    /**
     * 특정 테이블에 월별 파티션을 생성합니다.
     *
     * @param tableName      파티셔닝 대상 테이블명
     * @param partitionMonth 파티션 대상 월
     */
    public void createMonthlyPartition(String tableName, LocalDate partitionMonth) {
        LocalDate firstDay = partitionMonth.withDayOfMonth(1);
        LocalDate nextMonth = firstDay.plusMonths(1);
        String partitionName = tableName + "_" + firstDay.format(PARTITION_FORMAT);

        try {
            // 파티션 존재 여부 확인
            String checkSql = "SELECT COUNT(*) FROM pg_class WHERE relname = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, partitionName);

            if (count != null && count > 0) {
                log.debug("파티션 이미 존재: {}", partitionName);
                return;
            }

            // 테이블별 파티션 키 컬럼 결정
            String partitionColumn = getPartitionColumn(tableName);

            String createSql = String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s')",
                    partitionName, tableName,
                    firstDay.format(DATE_FORMAT),
                    nextMonth.format(DATE_FORMAT)
            );

            jdbcTemplate.execute(createSql);
            log.info("파티션 생성 완료: {}", partitionName);

        } catch (Exception e) {
            log.warn("파티션 생성 실패 (이미 존재할 수 있음): {} - {}", partitionName, e.getMessage());
        }
    }

    /**
     * 판매 거래 데이터를 S3 Glacier로 아카이빙합니다.
     */
    private void archiveSalesTransactions(LocalDateTime cutoff) {
        try {
            String query = "SELECT * FROM sales_transactions WHERE transaction_time < ? ORDER BY transaction_time";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, cutoff);

            if (rows.isEmpty()) {
                log.info("아카이빙 대상 판매 거래 없음");
                return;
            }

            byte[] csvData = convertToCsv(rows);
            String key = String.format("archive/sales_transactions/%s/data.csv.gz",
                    cutoff.toLocalDate().format(DATE_FORMAT));

            uploadToGlacier(key, csvData);
            log.info("판매 거래 아카이빙 완료: {}건", rows.size());

        } catch (Exception e) {
            log.error("판매 거래 아카이빙 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 일별 집계 데이터를 S3 Glacier로 아카이빙합니다.
     */
    private void archiveDailyAggregates(LocalDate cutoff) {
        try {
            String query = "SELECT * FROM daily_aggregates WHERE aggregate_date < ? ORDER BY aggregate_date";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, cutoff);

            if (rows.isEmpty()) {
                log.info("아카이빙 대상 일별 집계 없음");
                return;
            }

            byte[] csvData = convertToCsv(rows);
            String key = String.format("archive/daily_aggregates/%s/data.csv.gz",
                    cutoff.format(DATE_FORMAT));

            uploadToGlacier(key, csvData);
            log.info("일별 집계 아카이빙 완료: {}건", rows.size());

        } catch (Exception e) {
            log.error("일별 집계 아카이빙 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 감사 로그를 S3 Glacier로 아카이빙합니다.
     */
    private void archiveAuditLogs(LocalDateTime cutoff) {
        try {
            String query = "SELECT * FROM audit_logs WHERE event_time < ? ORDER BY event_time";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, cutoff);

            if (rows.isEmpty()) {
                log.info("아카이빙 대상 감사 로그 없음");
                return;
            }

            byte[] csvData = convertToCsv(rows);
            String key = String.format("archive/audit_logs/%s/data.csv.gz",
                    cutoff.toLocalDate().format(DATE_FORMAT));

            uploadToGlacier(key, csvData);
            log.info("감사 로그 아카이빙 완료: {}건", rows.size());

        } catch (Exception e) {
            log.error("감사 로그 아카이빙 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 만료된 파티션을 드롭합니다.
     */
    private void dropExpiredPartitions(String tableName, LocalDate cutoff) {
        try {
            // 만료된 파티션 목록 조회
            String query = "SELECT inhrelid::regclass::text AS partition_name " +
                    "FROM pg_inherits " +
                    "WHERE inhparent = ?::regclass";

            List<String> partitions = jdbcTemplate.queryForList(query, String.class, tableName);

            for (String partition : partitions) {
                // 파티션 이름에서 날짜 추출 (예: sales_transactions_2019_01)
                String datePart = partition.replace(tableName + "_", "");
                try {
                    LocalDate partitionDate = LocalDate.parse(datePart.replace("_", "-") + "-01",
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                    if (partitionDate.isBefore(cutoff)) {
                        String dropSql = "DROP TABLE IF EXISTS " + partition;
                        jdbcTemplate.execute(dropSql);
                        log.info("만료 파티션 삭제: {}", partition);
                    }
                } catch (Exception e) {
                    log.debug("파티션 날짜 파싱 스킵: {} - {}", partition, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("파티션 삭제 실패: {} - {}", tableName, e.getMessage(), e);
        }
    }

    /**
     * 데이터를 S3 (Glacier 스토리지 클래스)로 업로드합니다.
     * S3StorageService를 통해 업로드하며, 프로덕션 환경에서는 S3 Lifecycle Policy로
     * Glacier 전환이 자동 적용됩니다.
     */
    private void uploadToGlacier(String key, byte[] data) {
        s3StorageService.uploadFile(key, data, "text/csv");
        log.info("S3 아카이브 업로드 완료: key={} ({}bytes)", key, data.length);
    }

    /**
     * 쿼리 결과를 CSV 바이트 배열로 변환합니다.
     */
    private byte[] convertToCsv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // 헤더 작성
        String[] headers = rows.get(0).keySet().toArray(new String[0]);
        writer.println(String.join(",", headers));

        // 데이터 작성
        for (Map<String, Object> row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) line.append(",");
                Object value = row.get(headers[i]);
                if (value != null) {
                    String strValue = value.toString();
                    // CSV 이스케이프: 쉼표나 따옴표가 포함된 경우
                    if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
                        strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                    }
                    line.append(strValue);
                }
            }
            writer.println(line);
        }

        writer.flush();
        return baos.toByteArray();
    }

    /**
     * 테이블별 파티션 키 컬럼을 반환합니다.
     */
    private String getPartitionColumn(String tableName) {
        return switch (tableName) {
            case "sales_transactions" -> "transaction_time";
            case "daily_aggregates" -> "aggregate_date";
            case "audit_logs" -> "event_time";
            default -> throw new IllegalArgumentException("Unknown partitioned table: " + tableName);
        };
    }
}
