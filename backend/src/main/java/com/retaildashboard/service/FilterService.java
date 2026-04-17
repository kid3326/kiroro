package com.retaildashboard.service;

import com.retaildashboard.domain.Product;
import com.retaildashboard.domain.SavedFilter;
import com.retaildashboard.domain.User;
import com.retaildashboard.exception.ResourceNotFoundException;
import com.retaildashboard.repository.ProductRepository;
import com.retaildashboard.repository.SavedFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 필터 CRUD 및 상품 검색 서비스.
 * 저장된 필터의 생성, 조회, 삭제와 상품 검색 기능을 제공합니다.
 *
 * Requirements: 7.1, 7.2, 7.4, 7.5, 7.6, 7.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FilterService {

    private final SavedFilterRepository savedFilterRepository;
    private final ProductRepository productRepository;

    /**
     * 사용자의 저장된 필터 목록을 조회합니다.
     *
     * @param user 사용자
     * @return 저장된 필터 목록
     */
    public List<SavedFilter> getFiltersByUser(User user) {
        return savedFilterRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * 필터를 저장합니다.
     *
     * @param user           사용자
     * @param name           필터 이름
     * @param filterCriteria 필터 조건 (JSON 문자열)
     * @return 저장된 필터
     */
    @Transactional
    public SavedFilter saveFilter(User user, String name, String filterCriteria) {
        SavedFilter filter = SavedFilter.builder()
                .userId(user.getId())
                .name(name)
                .filterCriteria(filterCriteria)
                .build();

        SavedFilter saved = savedFilterRepository.save(filter);
        log.info("필터 저장: userId={}, filterName={}", user.getId(), name);
        return saved;
    }

    /**
     * ID로 필터를 조회합니다.
     * 사용자 본인의 필터만 조회할 수 있습니다.
     *
     * @param user     사용자
     * @param filterId 필터 ID
     * @return 저장된 필터
     * @throws ResourceNotFoundException 필터를 찾을 수 없는 경우
     */
    public SavedFilter getFilterById(User user, Integer filterId) {
        SavedFilter filter = savedFilterRepository.findById(filterId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedFilter", "id", filterId));

        if (!filter.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("SavedFilter", "id", filterId);
        }

        return filter;
    }

    /**
     * 필터를 삭제합니다.
     * 사용자 본인의 필터만 삭제할 수 있습니다.
     *
     * @param user     사용자
     * @param filterId 필터 ID
     * @throws ResourceNotFoundException 필터를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteFilter(User user, Integer filterId) {
        SavedFilter filter = getFilterById(user, filterId);
        savedFilterRepository.delete(filter);
        log.info("필터 삭제: userId={}, filterId={}", user.getId(), filterId);
    }

    /**
     * 상품을 이름 또는 SKU로 검색합니다.
     * 500ms 이내 응답을 목표로 합니다.
     *
     * Requirement 7.6, 7.7: 상품 검색 (이름/SKU, 500ms 이내 응답)
     *
     * @param query 검색어
     * @param page  페이지 번호
     * @param size  페이지 크기
     * @return 검색 결과 페이지
     */
    public Page<Product> searchProducts(String query, int page, int size) {
        return productRepository.searchByNameOrSku(query, PageRequest.of(page, size));
    }
}
