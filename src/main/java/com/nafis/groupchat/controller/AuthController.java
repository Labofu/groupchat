package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.LoginRequest;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return authService.register(user);
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/profile")
    public String profile() {
        return "Welcome to profile";
    }
}