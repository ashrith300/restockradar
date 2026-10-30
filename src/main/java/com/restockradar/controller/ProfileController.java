package com.restockradar.controller;

import com.restockradar.config.CustomUserDetails;
import com.restockradar.model.User;
import com.restockradar.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("user", principal.getUser());
        return "profile";
    }

    @PostMapping
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @RequestParam String ownerName,
                                @RequestParam String shopName,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes redirectAttributes) {
        try {
            User updated = userService.updateProfile(principal.getUser(), ownerName, shopName, email, phone);
            // Dynamically update the authenticated principal details in-memory
            principal.getUser().setOwnerName(updated.getOwnerName());
            principal.getUser().setShopName(updated.getShopName());
            principal.getUser().setEmail(updated.getEmail());
            principal.getUser().setPhone(updated.getPhone());

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
