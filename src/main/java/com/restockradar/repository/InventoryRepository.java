package com.restockradar.repository;

import com.restockradar.model.Inventory;
import com.restockradar.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductProductId(Long productId);
    
    @Query("SELECT i FROM Inventory i JOIN FETCH i.product WHERE i.product IN :products")
    List<Inventory> findByProductIn(@Param("products") List<Product> products);
    
    void deleteByProduct(Product product);
}
