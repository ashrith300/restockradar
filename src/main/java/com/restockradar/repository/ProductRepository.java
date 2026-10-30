package com.restockradar.repository;

import com.restockradar.model.Product;
import com.restockradar.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.supplier WHERE p.user = :user ORDER BY p.productName ASC")
    List<Product> findByUserOrderByProductNameAsc(@Param("user") User user);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.supplier WHERE p.productId = :productId AND p.user = :user")
    Optional<Product> findByProductIdAndUser(@Param("productId") Long productId, @Param("user") User user);
    
    long countByUser(User user);
}
