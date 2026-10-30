package com.restockradar.service;

import com.restockradar.dto.StockIntakeForm;
import com.restockradar.model.Product;
import com.restockradar.model.StockIntake;
import com.restockradar.model.Supplier;
import com.restockradar.model.User;
import com.restockradar.repository.ProductRepository;
import com.restockradar.repository.StockIntakeRepository;
import com.restockradar.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockIntakeService {

    private final StockIntakeRepository stockIntakeRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;

    public StockIntakeService(StockIntakeRepository stockIntakeRepository,
                              ProductRepository productRepository,
                              SupplierRepository supplierRepository,
                              InventoryService inventoryService) {
        this.stockIntakeRepository = stockIntakeRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryService = inventoryService;
    }

    public List<StockIntake> findByProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        return stockIntakeRepository.findByProductInOrderByReceivedAtDesc(products);
    }

    @Transactional
    public StockIntake processIntake(StockIntakeForm form, User user) {
        Product product = productRepository.findByProductIdAndUser(form.getProductId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Supplier supplier = null;
        if (form.getSupplierId() != null) {
            supplier = supplierRepository.findById(form.getSupplierId()).orElse(null);
        } else if (product.getSupplier() != null) {
            supplier = product.getSupplier();
        }

        StockIntake intake = new StockIntake();
        intake.setProduct(product);
        intake.setSupplier(supplier);
        intake.setQuantityReceived(form.getQuantityReceived());
        intake.setPurchaseUnitPrice(form.getPurchaseUnitPrice());
        if (form.getPurchaseUnitPrice() != null) {
            intake.setTotalCost(form.getPurchaseUnitPrice().multiply(BigDecimal.valueOf(form.getQuantityReceived())));
        }
        intake.setBatchNumber(form.getBatchNumber());
        intake.setExpiryDate(form.getExpiryDate());
        intake.setInvoiceNumber(form.getInvoiceNumber());
        intake.setNotes(form.getNotes());
        intake.setReceivedAt(LocalDateTime.now());

        StockIntake savedIntake = stockIntakeRepository.save(intake);

        // Update product inventory & details
        product.setCurrentStock(product.getCurrentStock() + form.getQuantityReceived());
        if (form.getBatchNumber() != null && !form.getBatchNumber().isBlank()) {
            product.setBatchNumber(form.getBatchNumber().trim());
        }
        if (form.getExpiryDate() != null) {
            product.setExpiryDate(form.getExpiryDate());
        }
        if (form.getPurchaseUnitPrice() != null && form.getPurchaseUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            product.setCostPrice(form.getPurchaseUnitPrice());
        }
        if (supplier != null && product.getSupplier() == null) {
            product.setSupplier(supplier);
        }

        Product updatedProduct = productRepository.save(product);
        inventoryService.recalcInventory(updatedProduct);
        inventoryService.generateRecommendation(updatedProduct);

        return savedIntake;
    }
}

