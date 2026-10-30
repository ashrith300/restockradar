package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.model.Product;
import com.restockradar.repository.ProductRepository;
import com.restockradar.service.InventoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public GlobalModelAttributes(ProductRepository productRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @ModelAttribute
    public void addGlobals(@AuthenticationPrincipal CustomUserDetails principal,
                            org.springframework.ui.Model model) {
        if (principal == null) {
            return;
        }
        model.addAttribute("shopName", principal.getUser().getShopName());
        model.addAttribute("ownerName", principal.getUser().getOwnerName());

        List<Product> products = productRepository.findByUserOrderByProductNameAsc(principal.getUser());
        model.addAttribute("activeAlertCount", inventoryService.getActiveAlerts(products).size());
    }
}
