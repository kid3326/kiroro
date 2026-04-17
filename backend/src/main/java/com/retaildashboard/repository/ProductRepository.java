package com.retaildashboard.repository;

import com.retaildashboard.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 상품 데이터 접근 Repository.
 * 상품 검색 기능을 제공합니다.
 *
 * Requirements: 7.6, 7.7
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    /**
     * 상품명 또는 SKU로 검색합니다 (LIKE 검색).
     * 500ms 이내 응답을 위해 인덱스를 활용합니다.
     *
     * @param query    검색어
     * @param pageable 페이지네이션
     * @return 검색 결과
     */
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Product> searchByNameOrSku(@Param("query") String query, Pageable pageable);
}
