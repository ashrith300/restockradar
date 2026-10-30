package com.restockradar.repository;

import com.restockradar.model.Product;
import com.restockradar.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    
    @Query("SELECT r FROM Report r JOIN FETCH r.product WHERE r.product IN :products ORDER BY r.generatedOn DESC")
    List<Report> findByProductInOrderByGeneratedOnDesc(@Param("products") List<Product> products);
    
    void deleteByProduct(Product product);
}
