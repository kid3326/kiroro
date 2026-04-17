package com.retaildashboard.controller;

import com.retaildashboard.config.Audited;
import com.retaildashboard.domain.Product;
import com.retaildashboard.domain.SavedFilter;
import com.retaildashboard.domain.User;
import com.retaildashboard.service.FilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 필터 및 상품 검색 REST API 컨트롤러.
 * 저장된 필터의 CRUD와 상품 검색 기능을 제공합니다.
 *
 * Requirements: 7.1, 7.2, 7.4, 7.5, 7.6, 7.7
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class FilterController {

    private final FilterService filterService;

    /**
     * 사용자의 저장된 필터 목록을 조회합니다.
     * GET /api/v1/filters
     *
     * @param authentication 인증 정보
     * @return 저장된 필터 목록
     */
    @GetMapping("/api/v1/filters")
    @Audited(eventType = "DATA_ACCESS", dataType = "PRODUCT", dataScope = "filter_list")
    public ResponseEntity<List<SavedFilter>> getFilters(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<SavedFilter> filters = filterService.getFiltersByUser(user);
        return ResponseEntity.ok(filters);
    }

    /**
     * 필터를 저장합니다.
     * POST /api/v1/filters
     *
     * 요청 본문 예시:
     * {
     *   "name": "내 필터",
     *   "filterCriteria": "{\"category\":\"전자제품\",\"brand\":\"삼성\"}"
     * }
     *
     * @param body           요청 본문 (name, filterCriteria)
     * @param authentication 인증 정보
     * @return 저장된 필터
     */
    @PostMapping("/api/v1/filters")
    @Audited(eventType = "FILTER_SAVE", dataType = "PRODUCT", dataScope = "filter_save")
    public ResponseEntity<SavedFilter> saveFilter(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        String name = body.get("name");
        String filterCriteria = body.get("filterCriteria");

        SavedFilter saved = filterService.saveFilter(user, name, filterCriteria);
        return ResponseEntity.ok(saved);
    }

    /**
     * ID로 필터를 조회합니다.
     * GET /api/v1/filters/{id}
     *
     * @param id             필터 ID
     * @param authentication 인증 정보
     * @return 저장된 필터
     */
    @GetMapping("/api/v1/filters/{id}")
    @Audited(eventType = "DATA_ACCESS", dataType = "PRODUCT", dataScope = "filter_load")
    public ResponseEntity<SavedFilter> getFilter(
            @PathVariable Integer id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        SavedFilter filter = filterService.getFilterById(user, id);
        return ResponseEntity.ok(filter);
    }

    /**
     * 필터를 삭제합니다.
     * DELETE /api/v1/filters/{id}
     *
     * @param id             필터 ID
     * @param authentication 인증 정보
     * @return 삭제 성공 메시지
     */
    @DeleteMapping("/api/v1/filters/{id}")
    @Audited(eventType = "FILTER_DELETE", dataType = "PRODUCT", dataScope = "filter_delete")
    public ResponseEntity<Map<String, String>> deleteFilter(
            @PathVariable Integer id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        filterService.deleteFilter(user, id);
        return ResponseEntity.ok(Map.of("message", "필터가 삭제되었습니다"));
    }

    /**
     * 상품을 이름 또는 SKU로 검색합니다.
     * GET /api/v1/search/products
     *
     * 500ms 이내 응답을 목표로 합니다.
     *
     * @param query 검색어
     * @param page  페이지 번호 (기본 0)
     * @param size  페이지 크기 (기본 50)
     * @return 검색 결과 페이지
     */
    @GetMapping("/api/v1/search/products")
    @Audited(eventType = "DATA_ACCESS", dataType = "PRODUCT", dataScope = "product_search")
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {

        Page<Product> results = filterService.searchProducts(query, page, size);
        return ResponseEntity.ok(results);
    }
}
