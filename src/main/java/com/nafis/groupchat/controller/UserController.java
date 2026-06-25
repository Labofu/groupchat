package com.nafis.groupchat.controller;

import com.nafis.groupchat.dto.UserProfileDTO;
import com.nafis.groupchat.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/user/{userId}")
    public UserProfileDTO getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfile(userId);
    }
}
