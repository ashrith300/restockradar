package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.dto.SaleForm;
import com.restockradar.model.Product;
import com.restockradar.model.Sale;
import com.restockradar.model.User;
import com.restockradar.service.ProductService;
import com.restockradar.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;
    private final ProductService productService;

    public SaleController(SaleService saleService, ProductService productService) {
        this.saleService = saleService;
        this.productService = productService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);
        List<Sale> sales = saleService.recentSales(products);

        model.addAttribute("products", products);
        model.addAttribute("sales", sales);
        model.addAttribute("saleForm", new SaleForm());
        return "sales";
    }

    @PostMapping("/new")
    public String record(@AuthenticationPrincipal CustomUserDetails principal,
                          @Valid @ModelAttribute SaleForm saleForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        User user = principal.getUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("products", productService.findAllForUser(user));
            model.addAttribute("sales", saleService.recentSales(productService.findAllForUser(user)));
            return "sales";
        }
        try {
            saleService.recordSale(saleForm, user);
            redirectAttributes.addFlashAttribute("successMessage", "Sale logged and stock updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/sales";
    }
}
