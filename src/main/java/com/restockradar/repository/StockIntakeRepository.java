package com.restockradar.repository;

import com.restockradar.model.Product;
import com.restockradar.model.StockIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockIntakeRepository extends JpaRepository<StockIntake, Long> {
    List<StockIntake> findByProductInOrderByReceivedAtDesc(List<Product> products);
    void deleteByProduct(Product product);
}

