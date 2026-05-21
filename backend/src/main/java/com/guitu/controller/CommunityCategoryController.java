package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.CommunityDtos.*;
import com.guitu.service.CommunityCategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CommunityCategoryController {
    private final CommunityCategoryService categoryService;

    public CommunityCategoryController(CommunityCategoryService categoryService) { this.categoryService = categoryService; }

    @GetMapping("/community/categories")
    public ApiResponse<List<CategoryResponse>> listEnabled() {
        return ApiResponse.ok(categoryService.listEnabled());
    }

    @GetMapping("/admin/community/categories")
    public ApiResponse<List<CategoryResponse>> listAll() {
        return ApiResponse.ok(categoryService.listAll());
    }

    @PostMapping("/admin/community/categories")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody SaveCategoryRequest req) {
        return ApiResponse.ok(categoryService.create(req));
    }

    @PutMapping("/admin/community/categories/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody SaveCategoryRequest req) {
        return ApiResponse.ok(categoryService.update(id, req));
    }
}
