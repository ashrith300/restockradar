package com.restockradar.service;

import com.restockradar.dto.ProductForm;
import com.restockradar.model.Category;
import com.restockradar.model.Product;
import com.restockradar.model.User;
import com.restockradar.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final ReorderRecommendationRepository reorderRecommendationRepository;
    private final StockAlertRepository stockAlertRepository;
    private final ReportRepository reportRepository;
    private final SupplierRepository supplierRepository;
    private final StockIntakeRepository stockIntakeRepository;

    public ProductService(ProductRepository productRepository,
                           CategoryRepository categoryRepository,
                           InventoryService inventoryService,
                           InventoryRepository inventoryRepository,
                           SaleRepository saleRepository,
                           ReorderRecommendationRepository reorderRecommendationRepository,
                           StockAlertRepository stockAlertRepository,
                           ReportRepository reportRepository,
                           SupplierRepository supplierRepository,
                           StockIntakeRepository stockIntakeRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
        this.saleRepository = saleRepository;
        this.reorderRecommendationRepository = reorderRecommendationRepository;
        this.stockAlertRepository = stockAlertRepository;
        this.reportRepository = reportRepository;
        this.supplierRepository = supplierRepository;
        this.stockIntakeRepository = stockIntakeRepository;
    }

    public List<Product> findAllForUser(User user) {
        return productRepository.findByUserOrderByProductNameAsc(user);
    }

    public Optional<Product> findForUser(Long productId, User user) {
        return productRepository.findByProductIdAndUser(productId, user);
    }

    @Transactional
    public Product create(ProductForm form, User user) {
        Product product = new Product();
        applyForm(product, form);
        product.setUser(user);
        Product saved = productRepository.save(product);
        inventoryService.recalcInventory(saved);
        return saved;
    }

    @Transactional
    public Product update(Product product, ProductForm form) {
        applyForm(product, form);
        Product saved = productRepository.save(product);
        inventoryService.recalcInventory(saved);
        return saved;
    }

    private void applyForm(Product product, ProductForm form) {
        product.setProductName(form.getProductName());
        product.setCostPrice(form.getCostPrice());
        product.setSellingPrice(form.getSellingPrice());
        product.setCurrentStock(form.getCurrentStock());
        product.setMinimumStock(form.getMinimumStock());
        product.setBarcode((form.getBarcode() == null || form.getBarcode().isBlank()) ? null : form.getBarcode());
        product.setBatchNumber((form.getBatchNumber() == null || form.getBatchNumber().isBlank()) ? null : form.getBatchNumber());
        if (form.getCategoryId() != null) {
            Category category = categoryRepository.findById(form.getCategoryId()).orElse(null);
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        product.setExpiryDate(form.getExpiryDate());
        if (form.getSupplierId() != null) {
            product.setSupplier(supplierRepository.findById(form.getSupplierId()).orElse(null));
        } else {
            product.setSupplier(null);
        }
    }

    @Transactional
    public void delete(Product product) {
        inventoryRepository.deleteByProduct(product);
        saleRepository.deleteByProduct(product);
        reorderRecommendationRepository.deleteByProduct(product);
        stockAlertRepository.deleteByProduct(product);
        reportRepository.deleteByProduct(product);
        stockIntakeRepository.deleteByProduct(product);
        productRepository.delete(product);
    }
}
