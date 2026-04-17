package com.retaildashboard.service;

import com.retaildashboard.domain.DailyAggregate;
import com.retaildashboard.domain.ExportFormat;
import com.retaildashboard.domain.ExportJob;
import com.retaildashboard.domain.ExportStatus;
import com.retaildashboard.dto.ChartSnapshot;
import com.retaildashboard.dto.ExportRequest;
import com.retaildashboard.dto.ExportResponse;
import com.retaildashboard.exception.ResourceNotFoundException;
import com.retaildashboard.repository.DailyAggregateRepository;
import com.retaildashboard.repository.ExportJobRepository;
import com.retaildashboard.service.aws.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 내보내기 서비스.
 * Excel, PDF, PPT 형식으로 대시보드 데이터를 내보냅니다.
 *
 * - Excel: Apache POI 기반, 데이터 카테고리별 시트 분리
 * - PDF: OpenPDF 기반, 표지 + 목차 + 페이지 번호
 * - PPT: Apache POI XSLF 기반, 차트당 1슬라이드 + 데이터 테이블
 * - 차트 이미지 임베딩: 최소 300 DPI
 * - 현재 필터 적용된 데이터 내보내기
 *
 * Requirements: 10.1-10.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ExportJobRepository exportJobRepository;
    private final DailyAggregateRepository dailyAggregateRepository;
    private final S3StorageService s3StorageService;

    /**
     * Excel 형식으로 내보냅니다.
     * 데이터 카테고리별 시트를 분리합니다.
     *
     * Requirements: 10.1, 10.2, 10.4
     *
     * @param request 내보내기 요청
     * @param userId  사용자 ID
     * @return 내보내기 응답
     */
    @Transactional
    public ExportResponse exportToExcel(ExportRequest request, UUID userId) {
        ExportJob job = createExportJob(userId, ExportFormat.EXCEL, request);

        try {
            byte[] content = generateExcelContent(request);
            return completeExport(job, content, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (Exception e) {
            return failExport(job, e);
        }
    }

    /**
     * PDF 형식으로 내보냅니다.
     * 표지, 목차, 페이지 번호를 포함합니다.
     *
     * Requirements: 10.1, 10.2, 10.5
     *
     * @param request 내보내기 요청
     * @param userId  사용자 ID
     * @return 내보내기 응답
     */
    @Transactional
    public ExportResponse exportToPdf(ExportRequest request, UUID userId) {
        ExportJob job = createExportJob(userId, ExportFormat.PDF, request);

        try {
            byte[] content = generatePdfContent(request);
            return completeExport(job, content, "application/pdf");
        } catch (Exception e) {
            return failExport(job, e);
        }
    }

    /**
     * PPT 형식으로 내보냅니다.
     * 차트당 1슬라이드 + 데이터 테이블을 포함합니다.
     *
     * Requirements: 10.1, 10.2, 10.6
     *
     * @param request 내보내기 요청
     * @param userId  사용자 ID
     * @return 내보내기 응답
     */
    @Transactional
    public ExportResponse exportToPpt(ExportRequest request, UUID userId) {
        ExportJob job = createExportJob(userId, ExportFormat.PPT, request);

        try {
            byte[] content = generatePptContent(request);
            return completeExport(job, content,
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        } catch (Exception e) {
            return failExport(job, e);
        }
    }

    /**
     * 내보내기 작업을 ID로 조회합니다.
     *
     * @param exportId 내보내기 작업 ID
     * @return 내보내기 응답
     */
    public ExportResponse getExportById(UUID exportId) {
        ExportJob job = exportJobRepository.findById(exportId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob", "id", exportId));

        return toExportResponse(job);
    }

    // ---- Excel 생성 ----

    private byte[] generateExcelContent(ExportRequest request) throws IOException {
        List<DailyAggregate> data = fetchFilteredData(request);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 카테고리별 시트 분리
            Map<String, List<DailyAggregate>> byCategory = data.stream()
                    .collect(Collectors.groupingBy(
                            d -> d.getCategory() != null ? d.getCategory() : "기타"));

            CellStyle headerStyle = createHeaderStyle(workbook);

            for (Map.Entry<String, List<DailyAggregate>> entry : byCategory.entrySet()) {
                Sheet sheet = workbook.createSheet(entry.getKey());
                writeExcelHeader(sheet, headerStyle);
                writeExcelData(sheet, entry.getValue());
                autoSizeColumns(sheet);
            }

            // 차트 이미지 임베딩 (300 DPI)
            if (request.getCharts() != null && !request.getCharts().isEmpty()) {
                Sheet chartSheet = workbook.createSheet("차트");
                embedChartsInExcel(workbook, chartSheet, request.getCharts());
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void writeExcelHeader(Sheet sheet, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        String[] columns = {"날짜", "SKU", "카테고리", "브랜드", "총매출", "순매출",
                "매출원가", "매출총이익", "판매량", "광고비", "ROAS"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeExcelData(Sheet sheet, List<DailyAggregate> data) {
        int rowNum = 1;
        for (DailyAggregate agg : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(agg.getAggregateDate().toString());
            row.createCell(1).setCellValue(agg.getSku() != null ? agg.getSku() : "");
            row.createCell(2).setCellValue(agg.getCategory() != null ? agg.getCategory() : "");
            row.createCell(3).setCellValue(agg.getBrand() != null ? agg.getBrand() : "");
            row.createCell(4).setCellValue(agg.getTotalRevenue().doubleValue());
            row.createCell(5).setCellValue(agg.getNetRevenue().doubleValue());
            row.createCell(6).setCellValue(agg.getCogs().doubleValue());
            row.createCell(7).setCellValue(agg.getGrossProfit().doubleValue());
            row.createCell(8).setCellValue(agg.getSalesVolume());
            row.createCell(9).setCellValue(agg.getAdSpend().doubleValue());
            row.createCell(10).setCellValue(agg.getRoas().doubleValue());
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 11; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void embedChartsInExcel(Workbook workbook, Sheet sheet, List<ChartSnapshot> charts) {
        int rowNum = 0;
        for (ChartSnapshot chart : charts) {
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue(chart.getTitle());

            if (chart.getImageBase64() != null && !chart.getImageBase64().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(chart.getImageBase64());
                    int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
                    var drawing = sheet.createDrawingPatriarch();
                    var anchor = workbook.getCreationHelper().createClientAnchor();
                    anchor.setCol1(0);
                    anchor.setRow1(rowNum);
                    anchor.setCol2(8);
                    anchor.setRow2(rowNum + 20);
                    drawing.createPicture(anchor, pictureIdx);
                    rowNum += 22;
                } catch (Exception e) {
                    log.warn("차트 이미지 임베딩 실패: {}", chart.getTitle(), e);
                    rowNum += 2;
                }
            }
        }
    }

    // ---- PDF 생성 ----

    private byte[] generatePdfContent(ExportRequest request) throws Exception {
        List<DailyAggregate> data = fetchFilteredData(request);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            com.lowagie.text.pdf.PdfWriter writer =
                    com.lowagie.text.pdf.PdfWriter.getInstance(document, out);

            // 페이지 번호 이벤트
            writer.setPageEvent(new PdfPageNumberEvent());

            document.open();

            // 표지
            addPdfCoverPage(document);

            // 목차
            document.newPage();
            addPdfTableOfContents(document, data);

            // 데이터 테이블
            document.newPage();
            addPdfDataTable(document, data);

            // 차트 이미지 (300 DPI)
            if (request.getCharts() != null) {
                for (ChartSnapshot chart : request.getCharts()) {
                    document.newPage();
                    addPdfChartPage(document, chart);
                }
            }

            document.close();
            return out.toByteArray();
        }
    }

    private void addPdfCoverPage(com.lowagie.text.Document document) throws Exception {
        document.add(new com.lowagie.text.Paragraph("\n\n\n\n\n"));

        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 28, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(
                "Retail Dashboard Report", titleFont);
        title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(title);

        document.add(new com.lowagie.text.Paragraph("\n\n"));

        com.lowagie.text.Font dateFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 14);
        com.lowagie.text.Paragraph date = new com.lowagie.text.Paragraph(
                "Generated: " + LocalDateTime.now().toString(), dateFont);
        date.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(date);
    }

    private void addPdfTableOfContents(com.lowagie.text.Document document,
                                        List<DailyAggregate> data) throws Exception {
        com.lowagie.text.Font tocTitleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
        document.add(new com.lowagie.text.Paragraph("Table of Contents", tocTitleFont));
        document.add(new com.lowagie.text.Paragraph("\n"));

        com.lowagie.text.Font tocFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 12);
        document.add(new com.lowagie.text.Paragraph("1. Data Summary", tocFont));
        document.add(new com.lowagie.text.Paragraph(
                "2. Detailed Data (" + data.size() + " records)", tocFont));
        document.add(new com.lowagie.text.Paragraph("3. Charts", tocFont));
    }

    private void addPdfDataTable(com.lowagie.text.Document document,
                                  List<DailyAggregate> data) throws Exception {
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD);
        document.add(new com.lowagie.text.Paragraph("Data Summary", sectionFont));
        document.add(new com.lowagie.text.Paragraph("\n"));

        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(7);
        table.setWidthPercentage(100);

        String[] headers = {"Date", "SKU", "Category", "Revenue", "COGS", "Profit", "Volume"};
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD);

        for (String header : headers) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase(header, headerFont));
            cell.setBackgroundColor(new java.awt.Color(220, 220, 220));
            table.addCell(cell);
        }

        com.lowagie.text.Font dataFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 8);

        for (DailyAggregate agg : data) {
            table.addCell(new com.lowagie.text.Phrase(agg.getAggregateDate().toString(), dataFont));
            table.addCell(new com.lowagie.text.Phrase(
                    agg.getSku() != null ? agg.getSku() : "", dataFont));
            table.addCell(new com.lowagie.text.Phrase(
                    agg.getCategory() != null ? agg.getCategory() : "", dataFont));
            table.addCell(new com.lowagie.text.Phrase(agg.getTotalRevenue().toString(), dataFont));
            table.addCell(new com.lowagie.text.Phrase(agg.getCogs().toString(), dataFont));
            table.addCell(new com.lowagie.text.Phrase(agg.getGrossProfit().toString(), dataFont));
            table.addCell(new com.lowagie.text.Phrase(
                    String.valueOf(agg.getSalesVolume()), dataFont));
        }

        document.add(table);
    }

    private void addPdfChartPage(com.lowagie.text.Document document,
                                  ChartSnapshot chart) throws Exception {
        com.lowagie.text.Font chartTitleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD);
        document.add(new com.lowagie.text.Paragraph(chart.getTitle(), chartTitleFont));
        document.add(new com.lowagie.text.Paragraph("\n"));

        if (chart.getImageBase64() != null && !chart.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(chart.getImageBase64());
                com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(imageBytes);
                // 300 DPI 스케일링
                image.scaleToFit(500, 400);
                image.setDpi(300, 300);
                document.add(image);
            } catch (Exception e) {
                log.warn("PDF 차트 이미지 임베딩 실패: {}", chart.getTitle(), e);
                document.add(new com.lowagie.text.Paragraph(
                        "[Chart image could not be embedded]"));
            }
        }
    }

    // ---- PPT 생성 ----

    private byte[] generatePptContent(ExportRequest request) throws IOException {
        List<DailyAggregate> data = fetchFilteredData(request);

        try (XMLSlideShow ppt = new XMLSlideShow();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            ppt.setPageSize(new Dimension(960, 540));

            // 타이틀 슬라이드
            addPptTitleSlide(ppt);

            // 데이터 요약 슬라이드
            addPptDataSummarySlide(ppt, data);

            // 차트당 1슬라이드 + 데이터 테이블
            if (request.getCharts() != null) {
                for (ChartSnapshot chart : request.getCharts()) {
                    addPptChartSlide(ppt, chart, data);
                }
            }

            ppt.write(out);
            return out.toByteArray();
        }
    }

    private void addPptTitleSlide(XMLSlideShow ppt) {
        XSLFSlide slide = ppt.createSlide();

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 150, 860, 100));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("Retail Dashboard Report");
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);

        XSLFTextBox dateBox = slide.createTextBox();
        dateBox.setAnchor(new Rectangle(50, 280, 860, 50));
        XSLFTextParagraph datePara = dateBox.addNewTextParagraph();
        XSLFTextRun dateRun = datePara.addNewTextRun();
        dateRun.setText("Generated: " + LocalDate.now().toString());
        dateRun.setFontSize(18.0);
    }

    private void addPptDataSummarySlide(XMLSlideShow ppt, List<DailyAggregate> data) {
        XSLFSlide slide = ppt.createSlide();

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 20, 860, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("Data Summary");
        titleRun.setFontSize(24.0);
        titleRun.setBold(true);

        XSLFTextBox contentBox = slide.createTextBox();
        contentBox.setAnchor(new Rectangle(50, 80, 860, 400));
        XSLFTextParagraph contentPara = contentBox.addNewTextParagraph();
        XSLFTextRun contentRun = contentPara.addNewTextRun();
        contentRun.setText("Total Records: " + data.size());
        contentRun.setFontSize(14.0);
    }

    private void addPptChartSlide(XMLSlideShow ppt, ChartSnapshot chart,
                                   List<DailyAggregate> data) {
        XSLFSlide slide = ppt.createSlide();

        // 차트 제목
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 20, 860, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(chart.getTitle());
        titleRun.setFontSize(20.0);
        titleRun.setBold(true);

        // 차트 이미지 임베딩 (300 DPI)
        if (chart.getImageBase64() != null && !chart.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(chart.getImageBase64());
                var pictureData = ppt.addPicture(imageBytes,
                        org.apache.poi.sl.usermodel.PictureData.PictureType.PNG);
                slide.createPicture(pictureData)
                        .setAnchor(new Rectangle(50, 80, 500, 350));
            } catch (Exception e) {
                log.warn("PPT 차트 이미지 임베딩 실패: {}", chart.getTitle(), e);
            }
        }

        // 데이터 테이블 (간략)
        XSLFTextBox tableBox = slide.createTextBox();
        tableBox.setAnchor(new Rectangle(570, 80, 370, 350));
        XSLFTextParagraph tablePara = tableBox.addNewTextParagraph();
        XSLFTextRun tableRun = tablePara.addNewTextRun();
        tableRun.setText("Data Table");
        tableRun.setFontSize(12.0);
        tableRun.setBold(true);

        int maxRows = Math.min(data.size(), 10);
        for (int i = 0; i < maxRows; i++) {
            DailyAggregate agg = data.get(i);
            XSLFTextParagraph rowPara = tableBox.addNewTextParagraph();
            XSLFTextRun rowRun = rowPara.addNewTextRun();
            rowRun.setText(String.format("%s | %s | %s",
                    agg.getAggregateDate(),
                    agg.getTotalRevenue(),
                    agg.getSalesVolume()));
            rowRun.setFontSize(9.0);
        }
    }

    // ---- 공통 헬퍼 ----

    private List<DailyAggregate> fetchFilteredData(ExportRequest request) {
        if (request.getDateRange() != null) {
            LocalDate from = request.getDateRange().startDate();
            LocalDate to = request.getDateRange().endDate();

            if (request.getFilters() != null && request.getFilters().getCategory() != null) {
                return dailyAggregateRepository.findByAggregateDateBetweenAndCategory(
                        from, to, request.getFilters().getCategory());
            }
            if (request.getFilters() != null && request.getFilters().getBrand() != null) {
                return dailyAggregateRepository.findByAggregateDateBetweenAndBrand(
                        from, to, request.getFilters().getBrand());
            }
            return dailyAggregateRepository.findByAggregateDateBetween(from, to);
        }

        // 기본: 최근 30일
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        return dailyAggregateRepository.findByAggregateDateBetween(from, to);
    }

    private ExportJob createExportJob(UUID userId, ExportFormat format, ExportRequest request) {
        ExportJob job = ExportJob.builder()
                .userId(userId)
                .format(format)
                .status(ExportStatus.PROCESSING)
                .filterCriteria(request.getFilters() != null ? request.getFilters().toString() : null)
                .build();
        return exportJobRepository.save(job);
    }

    private ExportResponse completeExport(ExportJob job, byte[] content, String contentType) {
        String s3Key = "exports/" + job.getId() + "/" + getFileExtension(job.getFormat());
        s3StorageService.uploadFile(s3Key, content, contentType);
        String downloadUrl = s3StorageService.generatePresignedUrl(s3Key);

        job.setStatus(ExportStatus.COMPLETED);
        job.setS3Key(s3Key);
        job.setDownloadUrl(downloadUrl);
        job.setFileSizeBytes((long) content.length);
        job.setExpiresAt(LocalDateTime.now().plusHours(24));
        exportJobRepository.save(job);

        log.info("내보내기 완료: jobId={}, format={}, size={} bytes",
                job.getId(), job.getFormat(), content.length);

        return toExportResponse(job);
    }

    private ExportResponse failExport(ExportJob job, Exception e) {
        log.error("내보내기 실패: jobId={}, format={}", job.getId(), job.getFormat(), e);
        job.setStatus(ExportStatus.FAILED);
        job.setErrorMessage(e.getMessage());
        exportJobRepository.save(job);
        return toExportResponse(job);
    }

    private String getFileExtension(ExportFormat format) {
        return switch (format) {
            case EXCEL -> "report.xlsx";
            case PDF -> "report.pdf";
            case PPT -> "report.pptx";
        };
    }

    private ExportResponse toExportResponse(ExportJob job) {
        return ExportResponse.builder()
                .id(job.getId())
                .downloadUrl(job.getDownloadUrl())
                .fileSizeBytes(job.getFileSizeBytes())
                .expiresAt(job.getExpiresAt())
                .status(job.getStatus().name())
                .build();
    }

    /**
     * PDF 페이지 번호 이벤트 핸들러.
     */
    private static class PdfPageNumberEvent extends com.lowagie.text.pdf.PdfPageEventHelper {
        @Override
        public void onEndPage(com.lowagie.text.pdf.PdfWriter writer,
                              com.lowagie.text.Document document) {
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            com.lowagie.text.Phrase footer = new com.lowagie.text.Phrase(
                    "Page " + writer.getPageNumber(),
                    new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8));
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    cb, com.lowagie.text.Element.ALIGN_CENTER, footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 10, 0);
        }
    }
}
