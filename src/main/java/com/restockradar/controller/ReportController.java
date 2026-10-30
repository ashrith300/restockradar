package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.model.Product;
import com.restockradar.model.Report;
import com.restockradar.model.User;
import com.restockradar.repository.ReportRepository;
import com.restockradar.service.InventoryService;
import com.restockradar.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final ReportRepository reportRepository;

    public ReportController(ProductService productService,
                             InventoryService inventoryService,
                             ReportRepository reportRepository) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.reportRepository = reportRepository;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);
        List<Report> reports = reportRepository.findByProductInOrderByGeneratedOnDesc(products);

        BigDecimal totalRevenue = reports.stream()
                .map(Report::getRevenue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("reports", reports);
        model.addAttribute("totalRevenue", totalRevenue);
        return "reports";
    }

    @PostMapping("/generate")
    public String generate(@AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttributes) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);
        inventoryService.generateReports(products);
        redirectAttributes.addFlashAttribute("successMessage", "Reports generated.");
        return "redirect:/reports";
    }
}
