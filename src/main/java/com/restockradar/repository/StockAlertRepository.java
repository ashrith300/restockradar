package com.restockradar.repository;

import com.restockradar.model.Product;
import com.restockradar.model.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    
    @Query("SELECT a FROM StockAlert a JOIN FETCH a.product WHERE a.product IN :products ORDER BY a.alertDate DESC")
    List<StockAlert> findByProductInOrderByAlertDateDesc(@Param("products") List<Product> products);
    
    @Query("SELECT a FROM StockAlert a JOIN FETCH a.product WHERE a.product IN :products AND a.status = :status ORDER BY a.alertDate DESC")
    List<StockAlert> findByProductInAndStatusOrderByAlertDateDesc(@Param("products") List<Product> products, @Param("status") StockAlert.AlertStatus status);
    
    List<StockAlert> findByProductProductIdAndStatus(Long productId, StockAlert.AlertStatus status);
    void deleteByProduct(Product product);
}
