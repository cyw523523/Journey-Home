package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.UserDtos;
import com.guitu.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class PublicUserController {
    private final UserService userService;

    public PublicUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDtos.PublicUserProfile> publicProfile(@PathVariable Long id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }
}
