package com.restockradar.repository;

import com.restockradar.model.Product;
import com.restockradar.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByProductProductIdOrderBySaleDateDesc(Long productId);
    List<Sale> findByProductProductIdAndSaleDateAfter(Long productId, LocalDateTime since);
    
    @Query("SELECT s FROM Sale s JOIN FETCH s.product WHERE s.product IN :products")
    List<Sale> findByProductIn(@Param("products") List<Product> products);
    
    @Query("SELECT s FROM Sale s JOIN FETCH s.product WHERE s.product IN :products ORDER BY s.saleDate DESC")
    List<Sale> findByProductInOrderBySaleDateDesc(@Param("products") List<Product> products);
    
    void deleteByProduct(Product product);
}
