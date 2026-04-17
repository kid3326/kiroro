package com.retaildashboard.repository;

import com.retaildashboard.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 재고 데이터 접근 Repository.
 *
 * Requirements: 5.5, 5.6
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    Optional<Inventory> findBySku(String sku);

    List<Inventory> findBySkuIn(List<String> skus);

    @Query("SELECT COALESCE(SUM(i.currentQuantity * i.unitCost), 0) FROM Inventory i")
    BigDecimal sumTotalInventoryValue();

    @Query("SELECT COALESCE(SUM(i.currentQuantity * i.unitCost), 0) FROM Inventory i WHERE i.sku = :sku")
    BigDecimal sumInventoryValueBySku(@Param("sku") String sku);
}
