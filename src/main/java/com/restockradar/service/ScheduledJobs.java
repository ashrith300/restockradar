package com.restockradar.service;

import com.restockradar.model.Product;
import com.restockradar.repository.ProductRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nightly job that refreshes reorder recommendations for every product,
 * so days-to-stockout / suggested reorder date stay current even without a new sale being logged.
 */
@Component
public class ScheduledJobs {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public ScheduledJobs(ProductRepository productRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    // Runs every day at 2:00 AM server time
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshRecommendationsNightly() {
        List<Product> allProducts = productRepository.findAll();
        inventoryService.generateRecommendationsForAll(allProducts);
    }
}
