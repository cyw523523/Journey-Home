package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.CommunityDtos.*;
import com.guitu.service.CommunityCategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CommunityCategoryController {
    private final CommunityCategoryService service;

    public CommunityCategoryController(CommunityCategoryService service) { this.service = service; }

    @GetMapping("/api/community/categories")
    public ApiResponse<List<CategoryResponse>> listEnabled() {
        return ApiResponse.ok(service.listEnabled());
    }

    @GetMapping("/api/admin/community/categories")
    public ApiResponse<List<CategoryResponse>> listAll() {
        return ApiResponse.ok(service.listAll());
    }

    @PostMapping("/api/admin/community/categories")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody SaveCategoryRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/api/admin/community/categories/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody SaveCategoryRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }
}
