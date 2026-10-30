package com.restockradar.service;

import com.restockradar.model.*;
import com.restockradar.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core business logic:
 *  - Stock health classification (Low / Healthy / Overstock)
 *  - Reorder recommendation engine (average daily sales, days-to-stockout, suggested qty/date)
 *  - Stock alert generation
 *  - Simple revenue / top-seller / dead-stock reporting
 *
 * Thresholds are simple, explainable math (no ML) per the project's design goals.
 */
@Service
public class InventoryService {

    private static final int SALES_WINDOW_DAYS = 30;      // window used to compute average daily sales
    private static final int TARGET_STOCK_DAYS = 14;       // how many days of stock we aim to hold after reorder
    private static final int SUPPLIER_LEAD_TIME_DAYS = 3;  // assumed days for a reorder to arrive
    private static final int DEAD_STOCK_DAYS = 30;          // no sales in this many days => dead stock

    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final StockAlertRepository stockAlertRepository;
    private final ReorderRecommendationRepository reorderRecommendationRepository;
    private final ReportRepository reportRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                             SaleRepository saleRepository,
                             StockAlertRepository stockAlertRepository,
                             ReorderRecommendationRepository reorderRecommendationRepository,
                             ReportRepository reportRepository) {
        this.inventoryRepository = inventoryRepository;
        this.saleRepository = saleRepository;
        this.stockAlertRepository = stockAlertRepository;
        this.reorderRecommendationRepository = reorderRecommendationRepository;
        this.reportRepository = reportRepository;
    }

    /** Classifies stock health based on current stock vs minimum stock. */
    public Inventory.StockStatus classify(int currentStock, int minimumStock) {
        if (minimumStock <= 0) minimumStock = 1;
        if (currentStock <= minimumStock) {
            return Inventory.StockStatus.Low;
        } else if (currentStock > minimumStock * 3) {
            return Inventory.StockStatus.Overstock;
        } else {
            return Inventory.StockStatus.Healthy;
        }
    }

    /** Recomputes and persists the Inventory row + raises/resolves Stock Alerts for a product. */
    @Transactional
    public Inventory recalcInventory(Product product) {
        Inventory inv = inventoryRepository.findByProductProductId(product.getProductId()).orElseGet(() -> {
            Inventory newInv = new Inventory();
            newInv.setProduct(product);
            return newInv;
        });

        Inventory.StockStatus status = classify(product.getCurrentStock(), product.getMinimumStock());
        inv.setAvailableStock(product.getCurrentStock());
        inv.setStockStatus(status);
        inv = inventoryRepository.save(inv);

        handleAlerts(product, status);
        return inv;
    }

    private void handleAlerts(Product product, Inventory.StockStatus status) {
        // Resolve any active alerts first
        List<StockAlert> activeAlerts = stockAlertRepository.findByProductProductIdAndStatus(product.getProductId(), StockAlert.AlertStatus.Active);

        if (status == Inventory.StockStatus.Healthy) {
            activeAlerts.forEach(a -> a.setStatus(StockAlert.AlertStatus.Resolved));
            stockAlertRepository.saveAll(activeAlerts);
            return;
        }

        StockAlert.AlertType type = status == Inventory.StockStatus.Low
                ? StockAlert.AlertType.Low
                : StockAlert.AlertType.Overstock;

        boolean alreadyActiveOfType = activeAlerts.stream().anyMatch(a -> a.getAlertType() == type);

        // Resolve alerts of the opposite type
        activeAlerts.stream()
                .filter(a -> a.getAlertType() != type)
                .forEach(a -> a.setStatus(StockAlert.AlertStatus.Resolved));
        stockAlertRepository.saveAll(activeAlerts);

        if (!alreadyActiveOfType) {
            StockAlert alert = new StockAlert();
            alert.setProduct(product);
            alert.setAlertType(type);
            alert.setStatus(StockAlert.AlertStatus.Active);
            if (type == StockAlert.AlertType.Low) {
                alert.setAlertMessage(String.format(
                        "%s is running low: %d units left (minimum %d).",
                        product.getProductName(), product.getCurrentStock(), product.getMinimumStock()));
            } else {
                alert.setAlertMessage(String.format(
                        "%s is overstocked: %d units on hand (minimum %d).",
                        product.getProductName(), product.getCurrentStock(), product.getMinimumStock()));
            }
            stockAlertRepository.save(alert);
        }
    }

    /** Computes average daily sales for a product over the trailing SALES_WINDOW_DAYS. */
    public BigDecimal averageDailySales(Product product) {
        LocalDateTime since = LocalDateTime.now().minusDays(SALES_WINDOW_DAYS);
        List<Sale> recentSales = saleRepository.findByProductProductIdAndSaleDateAfter(product.getProductId(), since);
        int totalSold = recentSales.stream().mapToInt(Sale::getQuantitySold).sum();
        if (totalSold <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalSold)
                .divide(BigDecimal.valueOf(SALES_WINDOW_DAYS), 2, RoundingMode.HALF_UP);
    }

    /** Runs the reorder-recommendation engine for a single product and persists the result. */
    @Transactional
    public ReorderRecommendation generateRecommendation(Product product) {
        BigDecimal avgDaily = averageDailySales(product);

        ReorderRecommendation rec = reorderRecommendationRepository.findFirstByProductProductIdOrderByReorderIdDesc(product.getProductId())
                .orElseGet(() -> {
                    ReorderRecommendation newRec = new ReorderRecommendation();
                    newRec.setProduct(product);
                    return newRec;
                });
        rec.setAverageDailySales(avgDaily);

        if (avgDaily.compareTo(BigDecimal.ZERO) <= 0) {
            // No recent sales data - can't project a stockout date
            rec.setDaysToStockout(null);
            rec.setSuggestedQuantity(0);
            rec.setReorderDate(null);
            rec.setRecommendationStatus(ReorderRecommendation.RecommendationStatus.Pending);
            return reorderRecommendationRepository.save(rec);
        }

        int daysToStockout = BigDecimal.valueOf(product.getCurrentStock())
                .divide(avgDaily, 0, RoundingMode.FLOOR)
                .intValue();

        int targetStock = avgDaily.multiply(BigDecimal.valueOf(TARGET_STOCK_DAYS))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
        int suggestedQuantity = Math.max(targetStock - product.getCurrentStock(), 0);

        int daysUntilReorder = Math.max(daysToStockout - SUPPLIER_LEAD_TIME_DAYS, 0);
        LocalDate reorderDate = LocalDate.now().plusDays(daysUntilReorder);

        rec.setDaysToStockout(daysToStockout);
        rec.setSuggestedQuantity(suggestedQuantity);
        rec.setReorderDate(reorderDate);
        rec.setRecommendationStatus(ReorderRecommendation.RecommendationStatus.Pending);

        return reorderRecommendationRepository.save(rec);
    }

    @Transactional
    public void generateRecommendationsForAll(List<Product> products) {
        for (Product p : products) {
            generateRecommendation(p);
        }
    }

    public List<ReorderRecommendation> getLatestRecommendations(List<Product> products) {
        return reorderRecommendationRepository.findByProductInOrderByReorderDateAsc(products);
    }

    public List<StockAlert> getActiveAlerts(List<Product> products) {
        return stockAlertRepository.findByProductInAndStatusOrderByAlertDateDesc(products, StockAlert.AlertStatus.Active);
    }

    public List<StockAlert> getAllAlerts(List<Product> products) {
        return stockAlertRepository.findByProductInOrderByAlertDateDesc(products);
    }

    @Transactional
    public void resolveAlert(Long alertId) {
        stockAlertRepository.findById(alertId).ifPresent(a -> {
            a.setStatus(StockAlert.AlertStatus.Resolved);
            stockAlertRepository.save(a);
        });
    }

    /** Generates a simple revenue / top-seller / dead-stock report for each product. */
    @Transactional
    public List<Report> generateReports(List<Product> products) {
        // delete previous reports to avoid duplication and infinite database growth
        List<Report> oldReports = reportRepository.findByProductInOrderByGeneratedOnDesc(products);
        reportRepository.deleteAll(oldReports);

        List<Report> reports = products.stream().map(product -> {
            List<Sale> allSales = saleRepository.findByProductProductIdOrderBySaleDateDesc(product.getProductId());

            BigDecimal revenue = allSales.stream()
                    .map(Sale::getTotalAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean hasRecentSale = allSales.stream()
                    .anyMatch(s -> s.getSaleDate() != null &&
                            s.getSaleDate().isAfter(LocalDateTime.now().minusDays(DEAD_STOCK_DAYS)));

            Report report = new Report();
            report.setProduct(product);
            report.setRevenue(revenue);
            report.setDeadStock(!hasRecentSale);
            report.setTopSelling(false); // determined relative to siblings below
            return report;
        }).toList();

        // Mark the top seller (highest revenue, if any sales exist) as top_selling = true
        reports.stream()
                .filter(r -> r.getRevenue() != null && r.getRevenue().compareTo(BigDecimal.ZERO) > 0)
                .max((a, b) -> a.getRevenue().compareTo(b.getRevenue()))
                .ifPresent(top -> top.setTopSelling(true));

        return reportRepository.saveAll(reports);
    }
}
