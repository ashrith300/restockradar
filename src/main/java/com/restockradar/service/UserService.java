package com.restockradar.service;

import com.restockradar.dto.RegisterForm;
import com.restockradar.model.User;
import com.restockradar.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterForm form) {
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = new User();
        user.setOwnerName(form.getOwnerName());
        user.setShopName(form.getShopName());
        user.setEmail(form.getEmail());
        user.setPhone((form.getPhone() == null || form.getPhone().isBlank()) ? null : form.getPhone());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(User user, String ownerName, String shopName, String email, String phone) {
        if (userRepository.existsByEmail(email) && !user.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        user.setOwnerName(ownerName);
        user.setShopName(shopName);
        user.setEmail(email);
        user.setPhone((phone == null || phone.isBlank()) ? null : phone);
        return userRepository.save(user);
    }
}
