package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.dto.ProductForm;
import com.restockradar.model.Product;
import com.restockradar.model.User;
import com.restockradar.service.CategoryService;
import com.restockradar.service.ProductService;
import com.restockradar.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;

    public ProductController(ProductService productService, CategoryService categoryService, SupplierService supplierService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = principal.getUser();
        model.addAttribute("products", productService.findAllForUser(user));
        return "products";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("suppliers", supplierService.findAll());
        return "product-form";
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal CustomUserDetails principal,
                          @Valid @ModelAttribute ProductForm productForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("suppliers", supplierService.findAll());
            return "product-form";
        }
        productService.create(productForm, principal.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "Product added successfully.");
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal CustomUserDetails principal,
                            @PathVariable Long id, Model model) {
        Product product = productService.findForUser(id, principal.getUser())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        ProductForm form = new ProductForm();
        form.setProductId(product.getProductId());
        form.setProductName(product.getProductName());
        form.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null);
        form.setCostPrice(product.getCostPrice());
        form.setSellingPrice(product.getSellingPrice());
        form.setCurrentStock(product.getCurrentStock());
        form.setMinimumStock(product.getMinimumStock());
        form.setBarcode(product.getBarcode());
        form.setBatchNumber(product.getBatchNumber());
        form.setSupplierId(product.getSupplier() != null ? product.getSupplier().getSupplierId() : null);
        form.setExpiryDate(product.getExpiryDate());

        model.addAttribute("productForm", form);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("suppliers", supplierService.findAll());
        return "product-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@AuthenticationPrincipal CustomUserDetails principal,
                          @PathVariable Long id,
                          @Valid @ModelAttribute ProductForm productForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("suppliers", supplierService.findAll());
            return "product-form";
        }
        Product product = productService.findForUser(id, principal.getUser())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        productService.update(product, productForm);
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully.");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetails principal,
                          @PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
        productService.findForUser(id, principal.getUser()).ifPresent(productService::delete);
        redirectAttributes.addFlashAttribute("successMessage", "Product deleted.");
        return "redirect:/products";
    }
}
