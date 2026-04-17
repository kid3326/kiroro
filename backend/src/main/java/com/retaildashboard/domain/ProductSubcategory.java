package com.retaildashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 서브카테고리 엔티티.
 * product_subcategories 테이블과 매핑됩니다.
 *
 * Requirements: 5.1
 */
@Entity
@Table(name = "product_subcategories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSubcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;
}
