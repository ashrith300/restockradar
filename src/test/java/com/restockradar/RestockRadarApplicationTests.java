package com.restockradar;

import com.restockradar.controller.AuthController;
import com.restockradar.dto.RegisterForm;
import com.restockradar.model.Product;
import com.restockradar.model.ReorderRecommendation;
import com.restockradar.model.Report;
import com.restockradar.model.User;
import com.restockradar.repository.*;
import com.restockradar.service.InventoryService;
import com.restockradar.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import com.restockradar.config.CustomUserDetails;
import com.restockradar.model.Category;
import com.restockradar.model.StockAlert;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RestockRadarApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReorderRecommendationRepository reorderRecommendationRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    void testRegistrationEmailDuplicateShowsFieldError() throws Exception {
        // Register an initial user
        RegisterForm form1 = new RegisterForm();
        form1.setOwnerName("Owner One");
        form1.setShopName("Shop One");
        form1.setEmail("duplicate@test.com");
        form1.setPassword("password");
        userService.register(form1);

        // Attempt to register again with same email via POST
        mockMvc.perform(post("/register")
                .param("ownerName", "Owner Two")
                .param("shopName", "Shop Two")
                .param("email", "duplicate@test.com")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"));
    }

    @Test
    void testReorderRecommendationUpdatesInsteadOfCreatesDuplicates() {
        // Create user
        User user = new User();
        user.setOwnerName("Test Owner");
        user.setShopName("Test Shop");
        user.setEmail("reorder@test.com");
        user.setPassword("password");
        user = userRepository.save(user);

        // Create product
        Product product = new Product();
        product.setUser(user);
        product.setProductName("Test Product");
        product.setCostPrice(BigDecimal.TEN);
        product.setSellingPrice(BigDecimal.valueOf(15));
        product.setCurrentStock(5);
        product.setMinimumStock(10);
        product = productRepository.save(product);

        // Generate recommendation once
        inventoryService.generateRecommendation(product);
        List<ReorderRecommendation> recs1 = reorderRecommendationRepository.findByProductInOrderByReorderDateAsc(List.of(product));
        assertEquals(1, recs1.size());

        // Generate recommendation again
        inventoryService.generateRecommendation(product);
        List<ReorderRecommendation> recs2 = reorderRecommendationRepository.findByProductInOrderByReorderDateAsc(List.of(product));
        assertEquals(1, recs2.size(), "Should update the existing recommendation instead of creating a duplicate");
    }

    @Test
    void testGenerateReportsClearsPreviousReports() {
        // Create user
        User user = new User();
        user.setOwnerName("Test Owner");
        user.setShopName("Test Shop");
        user.setEmail("reports@test.com");
        user.setPassword("password");
        user = userRepository.save(user);

        // Create product
        Product product = new Product();
        product.setUser(user);
        product.setProductName("Report Product");
        product.setCostPrice(BigDecimal.TEN);
        product.setSellingPrice(BigDecimal.valueOf(15));
        product.setCurrentStock(5);
        product.setMinimumStock(10);
        product = productRepository.save(product);

        // Generate reports once
        inventoryService.generateReports(List.of(product));
        List<Report> reports1 = reportRepository.findByProductInOrderByGeneratedOnDesc(List.of(product));
        assertEquals(1, reports1.size());

        // Generate reports again
        inventoryService.generateReports(List.of(product));
        List<Report> reports2 = reportRepository.findByProductInOrderByGeneratedOnDesc(List.of(product));
        assertEquals(1, reports2.size(), "Should clear old reports and only keep the latest generated ones");
    }

    @Test
    void testCompleteUserFlow() throws Exception {
        // 1. Create a user
        User userEntity = new User();
        userEntity.setOwnerName("Flow Owner");
        userEntity.setShopName("Flow Shop");
        userEntity.setEmail("flow@test.com");
        userEntity.setPassword("password");
        userEntity = userRepository.save(userEntity);
        CustomUserDetails userDetails = new CustomUserDetails(userEntity);

        // 2. GET /dashboard
        mockMvc.perform(get("/dashboard").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("totalProducts", "lowCount", "healthyCount", "overstockCount"));

        // 3. GET /categories
        mockMvc.perform(get("/categories").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("categories"))
                .andExpect(model().attributeExists("categories"));

        // 4. POST /categories/new
        mockMvc.perform(post("/categories/new")
                .param("categoryName", "Beverages")
                .param("description", "Drinks")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        List<Category> categories = categoryRepository.findAll();
        assertFalse(categories.isEmpty());
        Category category = categories.stream()
                .filter(c -> c.getCategoryName().equals("Beverages"))
                .findFirst().orElseThrow();

        // 5. GET /products/new
        mockMvc.perform(get("/products/new").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(model().attributeExists("categories"));

        // 6. POST /products/new
        mockMvc.perform(post("/products/new")
                .param("productName", "Coke")
                .param("categoryId", category.getCategoryId().toString())
                .param("costPrice", "10.00")
                .param("sellingPrice", "15.00")
                .param("currentStock", "50")
                .param("minimumStock", "10")
                .param("barcode", "12345")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        List<Product> products = productRepository.findByUserOrderByProductNameAsc(userEntity);
        assertEquals(1, products.size());
        Product product = products.get(0);

        // 7. GET /products
        mockMvc.perform(get("/products").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeExists("products"));

        // 8. GET /products/{id}/edit
        mockMvc.perform(get("/products/" + product.getProductId() + "/edit").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(model().attributeExists("productForm", "categories"));

        // 9. POST /products/{id}/edit
        mockMvc.perform(post("/products/" + product.getProductId() + "/edit")
                .param("productName", "Coke Updated")
                .param("categoryId", category.getCategoryId().toString())
                .param("costPrice", "11.00")
                .param("sellingPrice", "16.00")
                .param("currentStock", "45")
                .param("minimumStock", "10")
                .param("barcode", "12345")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        // Refresh product reference
        product = productRepository.findById(product.getProductId()).orElseThrow();

        // 10. GET /sales
        mockMvc.perform(get("/sales").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("sales"))
                .andExpect(model().attributeExists("products", "sales"));

        // 11. POST /sales/new
        mockMvc.perform(post("/sales/new")
                .param("productId", product.getProductId().toString())
                .param("quantitySold", "5")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales"));

        // 12. GET /reorders
        mockMvc.perform(get("/reorders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("reorders"))
                .andExpect(model().attributeExists("recommendations"));

        // 13. POST /reorders/generate
        mockMvc.perform(post("/reorders/generate")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reorders"));

        // 14. GET /alerts
        mockMvc.perform(get("/alerts").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("alerts"))
                .andExpect(model().attributeExists("alerts"));

        // 15. POST /alerts/{id}/resolve
        List<StockAlert> alerts = inventoryService.getAllAlerts(List.of(product));
        if (!alerts.isEmpty()) {
            mockMvc.perform(post("/alerts/" + alerts.get(0).getAlertId() + "/resolve")
                    .with(csrf()).with(user(userDetails)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/alerts"));
        }

        // 16. GET /reports
        mockMvc.perform(get("/reports").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"))
                .andExpect(model().attributeExists("reports", "totalRevenue"));

        // 17. POST /reports/generate
        mockMvc.perform(post("/reports/generate")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports"));

        // 18. POST /products/{id}/delete
        mockMvc.perform(post("/products/" + product.getProductId() + "/delete")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        // 19. POST /categories/{id}/delete
        mockMvc.perform(post("/categories/" + category.getCategoryId() + "/delete")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));
    }

    @Test
    void testSupplierCrudOperations() throws Exception {
        User user = new User();
        user.setOwnerName("Supplier User");
        user.setShopName("Supplier Shop");
        user.setEmail("supplier-test@test.com");
        user.setPassword("password");
        user = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // 1. GET /suppliers (should display current suppliers)
        mockMvc.perform(get("/suppliers").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("suppliers"))
                .andExpect(model().attributeExists("suppliers", "supplierForm"));

        // 2. POST /suppliers/new (create new supplier)
        mockMvc.perform(post("/suppliers/new")
                .param("supplierName", "Test Supplier Inc")
                .param("contactName", "John Doe")
                .param("email", "john@testsupplier.com")
                .param("phone", "9876543210")
                .param("address", "123 Test Street")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers"));

        // Find the saved supplier
        com.restockradar.model.Supplier supplier = supplierRepository.findAll().stream()
                .filter(s -> s.getSupplierName().equals("Test Supplier Inc"))
                .findFirst().orElse(null);
        assertNotNull(supplier);
        assertEquals("John Doe", supplier.getContactName());

        // 3. POST /suppliers/{id}/delete
        mockMvc.perform(post("/suppliers/" + supplier.getSupplierId() + "/delete")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers"));
    }

    @Test
    void testProfileUpdate() throws Exception {
        User user = new User();
        user.setOwnerName("Original Owner");
        user.setShopName("Original Shop");
        user.setEmail("original@test.com");
        user.setPassword("password");
        user = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // 1. GET /profile
        mockMvc.perform(get("/profile").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"));

        // 2. POST /profile (update details)
        mockMvc.perform(post("/profile")
                .param("ownerName", "Updated Owner")
                .param("shopName", "Updated Shop")
                .param("email", "updated@test.com")
                .param("phone", "1122334455")
                .with(csrf()).with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        // Verify entity updated in DB
        User updated = userRepository.findById(user.getUserId()).orElse(null);
        assertNotNull(updated);
        assertEquals("Updated Owner", updated.getOwnerName());
        assertEquals("Updated Shop", updated.getShopName());
        assertEquals("updated@test.com", updated.getEmail());
    }
}
