package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.UserDtos;
import com.guitu.repository.UserRepository;
import com.guitu.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class PublicUserController {
    private final UserService userService;
    private final UserRepository userRepo;

    public PublicUserController(UserService userService, UserRepository userRepo) {
        this.userService = userService;
        this.userRepo = userRepo;
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDtos.PublicUserProfile> publicProfile(@PathVariable Long id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }

    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> searchUsers(@RequestParam String keyword, @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return ApiResponse.ok(userRepo.findByNicknameStartingWith(keyword, pageable).stream()
                .<Map<String, Object>>map(u -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", u.getId());
                    m.put("nickname", u.getNickname());
                    m.put("avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
                    return m;
                }).toList());
    }
}
