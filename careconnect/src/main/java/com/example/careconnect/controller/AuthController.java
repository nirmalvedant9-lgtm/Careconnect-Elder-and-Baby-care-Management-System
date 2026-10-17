package com.example.careconnect.controller;

import com.example.careconnect.model.User;
import com.example.careconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest request) {
        try {
            String role = request.accountType != null ? request.accountType.toUpperCase() : "FAMILY";
            if ("USER".equals(role)) role = "FAMILY";
            User newUser = new User(
                request.fullName != null ? request.fullName : request.name,
                request.email,
                passwordEncoder.encode(request.password),
                role
            );
            userRepository.save(newUser);
            return ResponseEntity.ok("User registered successfully and saved to database!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    public static class RegisterRequest {
        public String fullName;
        public String name;
        public String email;
        public String password;
        public String accountType;
        public String role;
    }
}