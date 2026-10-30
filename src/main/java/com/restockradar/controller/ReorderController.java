package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.model.Product;
import com.restockradar.model.ReorderRecommendation;
import com.restockradar.model.User;
import com.restockradar.service.InventoryService;
import com.restockradar.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reorders")
public class ReorderController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public ReorderController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);
        List<ReorderRecommendation> recommendations = inventoryService.getLatestRecommendations(products);

        model.addAttribute("recommendations", recommendations);
        return "reorders";
    }

    @PostMapping("/generate")
    public String generate(@AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttributes) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);
        inventoryService.generateRecommendationsForAll(products);
        redirectAttributes.addFlashAttribute("successMessage", "Reorder recommendations refreshed for all products.");
        return "redirect:/reorders";
    }
}
