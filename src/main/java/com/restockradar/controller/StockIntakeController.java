package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.dto.StockIntakeForm;
import com.restockradar.model.Product;
import com.restockradar.model.StockIntake;
import com.restockradar.model.User;
import com.restockradar.service.ProductService;
import com.restockradar.service.StockIntakeService;
import com.restockradar.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/intake")
public class StockIntakeController {

    private final StockIntakeService stockIntakeService;
    private final ProductService productService;
    private final SupplierService supplierService;

    public StockIntakeController(StockIntakeService stockIntakeService,
                                 ProductService productService,
                                 SupplierService supplierService) {
        this.stockIntakeService = stockIntakeService;
        this.productService = productService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String intakePage(@AuthenticationPrincipal CustomUserDetails principal,
                             @RequestParam(required = false) Long productId,
                             Model model) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);
        List<StockIntake> intakes = stockIntakeService.findByProducts(products);

        StockIntakeForm form = new StockIntakeForm();
        if (productId != null) {
            form.setProductId(productId);
            products.stream()
                    .filter(p -> p.getProductId().equals(productId))
                    .findFirst()
                    .ifPresent(p -> {
                        if (p.getSupplier() != null) {
                            form.setSupplierId(p.getSupplier().getSupplierId());
                        }
                        form.setPurchaseUnitPrice(p.getCostPrice());
                        form.setBatchNumber(p.getBatchNumber());
                        form.setExpiryDate(p.getExpiryDate());
                    });
        }

        model.addAttribute("stockIntakeForm", form);
        model.addAttribute("products", products);
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("intakes", intakes);
        return "stock-intake";
    }

    @PostMapping
    public String processIntake(@AuthenticationPrincipal CustomUserDetails principal,
                                @Valid @ModelAttribute("stockIntakeForm") StockIntakeForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User user = principal.getUser();
        List<Product> products = productService.findAllForUser(user);

        if (bindingResult.hasErrors()) {
            model.addAttribute("products", products);
            model.addAttribute("suppliers", supplierService.findAll());
            model.addAttribute("intakes", stockIntakeService.findByProducts(products));
            return "stock-intake";
        }

        try {
            StockIntake saved = stockIntakeService.processIntake(form, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Received " + saved.getQuantityReceived() + " units for " + saved.getProduct().getProductName() + ". Stock updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/intake";
    }
}

