package com.restockradar.controller;

import com.restockradar.model.Supplier;
import com.restockradar.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("supplierForm", new Supplier());
        return "suppliers";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("supplierForm") Supplier supplier,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("suppliers", supplierService.findAll());
            return "suppliers";
        }
        supplierService.save(supplier);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier added successfully.");
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Supplier deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete supplier — they might have active products linked to them.");
        }
        return "redirect:/suppliers";
    }
}
