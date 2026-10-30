package com.restockradar.service;

import com.restockradar.dto.SaleForm;
import com.restockradar.model.Product;
import com.restockradar.model.Sale;
import com.restockradar.model.User;
import com.restockradar.repository.ProductRepository;
import com.restockradar.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public SaleService(SaleRepository saleRepository,
                        ProductRepository productRepository,
                        InventoryService inventoryService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Sale recordSale(SaleForm form, User user) {
        Product product = productRepository.findByProductIdAndUser(form.getProductId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (form.getQuantitySold() > product.getCurrentStock()) {
            throw new IllegalArgumentException(
                    "Cannot sell " + form.getQuantitySold() + " units — only " + product.getCurrentStock() + " in stock.");
        }

        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setQuantitySold(form.getQuantitySold());
        sale.setSellingPrice(product.getSellingPrice());
        Sale saved = saleRepository.save(sale);

        product.setCurrentStock(product.getCurrentStock() - form.getQuantitySold());
        productRepository.save(product);

        inventoryService.recalcInventory(product);
        inventoryService.generateRecommendation(product);

        return saved;
    }

    public List<Sale> recentSales(List<Product> products) {
        return saleRepository.findByProductInOrderBySaleDateDesc(products);
    }
}
