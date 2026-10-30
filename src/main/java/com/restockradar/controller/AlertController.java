package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.model.Product;
import com.restockradar.model.StockAlert;
import com.restockradar.model.User;
import com.restockradar.service.InventoryService;
import com.restockradar.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/alerts")
public class AlertController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public AlertController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);
        List<StockAlert> alerts = inventoryService.getAllAlerts(products);

        model.addAttribute("alerts", alerts);
        return "alerts";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        inventoryService.resolveAlert(id);
        redirectAttributes.addFlashAttribute("successMessage", "Alert marked as resolved.");
        return "redirect:/alerts";
    }
}
