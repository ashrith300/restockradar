package com.restockradar.repository;

import com.restockradar.model.Product;
import com.restockradar.model.ReorderRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReorderRecommendationRepository extends JpaRepository<ReorderRecommendation, Long> {
    
    @Query("SELECT r FROM ReorderRecommendation r JOIN FETCH r.product WHERE r.product IN :products ORDER BY r.reorderDate ASC")
    List<ReorderRecommendation> findByProductInOrderByReorderDateAsc(@Param("products") List<Product> products);
    
    Optional<ReorderRecommendation> findFirstByProductProductIdOrderByReorderIdDesc(Long productId);
    void deleteByProduct(Product product);
}
