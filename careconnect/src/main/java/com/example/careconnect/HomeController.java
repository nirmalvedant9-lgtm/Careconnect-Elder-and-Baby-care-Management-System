package com.example.careconnect;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/careconnect")
    public String home() {
        return "Welcome to CareConnect - Elder Care and Baby Care Management System!";
    }
}