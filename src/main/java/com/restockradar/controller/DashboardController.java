package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.model.Inventory;
import com.restockradar.model.Product;
import com.restockradar.model.StockAlert;
import com.restockradar.model.User;
import com.restockradar.model.Sale;
import com.restockradar.repository.InventoryRepository;
import com.restockradar.repository.SaleRepository;
import com.restockradar.service.InventoryService;
import com.restockradar.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;

    public DashboardController(ProductService productService,
                                InventoryService inventoryService,
                                InventoryRepository inventoryRepository,
                                SaleRepository saleRepository) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
        this.saleRepository = saleRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = principal.getUser();
        
        List<Product> products = productService.findAllForUser(user);
        List<Inventory> inventories = inventoryRepository.findByProductIn(products);

        Map<Long, Inventory> inventoryByProduct = inventories.stream()
                .collect(Collectors.toMap(i -> i.getProduct().getProductId(), i -> i));

        long lowCount = inventories.stream().filter(i -> i.getStockStatus() == Inventory.StockStatus.Low).count();
        long healthyCount = inventories.stream().filter(i -> i.getStockStatus() == Inventory.StockStatus.Healthy).count();
        long overstockCount = inventories.stream().filter(i -> i.getStockStatus() == Inventory.StockStatus.Overstock).count();

        List<StockAlert> activeAlerts = inventoryService.getActiveAlerts(products);

        // Fetch sales for this user's products to power visual charts
        List<Sale> allSales = products.isEmpty() ? List.of() : saleRepository.findByProductIn(products);

        // Chart 1: Weekly Sales Trend (Last 7 Days)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
        List<String> dates = new java.util.ArrayList<>();
        List<BigDecimal> revenues = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));

            BigDecimal dayRevenue = allSales.stream()
                    .filter(s -> s.getSaleDate() != null && s.getSaleDate().toLocalDate().equals(date))
                    .map(Sale::getTotalAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenues.add(dayRevenue);
        }

        // Chart 2: Top 5 Selling Products by Revenue
        Map<Product, BigDecimal> productRevenue = allSales.stream()
                .collect(Collectors.groupingBy(
                        Sale::getProduct,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Sale::getTotalAmount,
                                BigDecimal::add
                        )
                ));

        List<String> topProductNames = productRevenue.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> e.getKey().getProductName())
                .collect(Collectors.toList());

        List<BigDecimal> topProductRevenues = productRevenue.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        model.addAttribute("shopName", user.getShopName());
        model.addAttribute("products", products);
        model.addAttribute("inventoryByProduct", inventoryByProduct);
        model.addAttribute("lowCount", lowCount);
        model.addAttribute("healthyCount", healthyCount);
        model.addAttribute("overstockCount", overstockCount);
        model.addAttribute("totalProducts", products.size());
        model.addAttribute("activeAlerts", activeAlerts);

        List<Product> expiringProducts = products.stream()
                .filter(p -> p.getExpiryDate() != null && (p.isExpired() || p.isExpiringSoon()))
                .sorted((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()))
                .collect(Collectors.toList());
        model.addAttribute("expiringProducts", expiringProducts);

        // Pass lists for Javascript to load Chart.js
        model.addAttribute("chartDates", dates);
        model.addAttribute("chartRevenues", revenues);
        model.addAttribute("chartProductNames", topProductNames);
        model.addAttribute("chartProductRevenues", topProductRevenues);

        return "dashboard";
    }
}
