package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.AnimalGender;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.AnimalType;
import com.guitu.dto.AnimalDtos;
import com.guitu.service.AnimalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animals")
public class AnimalController {
    private final AnimalService animalService;

    public AnimalController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AnimalDtos.AnimalResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AnimalType type,
            @RequestParam(required = false) AnimalGender gender,
            @RequestParam(required = false) AnimalStatus status,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(animalService.listPublic(keyword, type, gender, status, region, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AnimalDtos.AnimalResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(animalService.detail(id));
    }

    @PostMapping
    public ApiResponse<AnimalDtos.AnimalResponse> create(@Valid @RequestBody AnimalDtos.SaveAnimalRequest request) {
        return ApiResponse.ok(animalService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AnimalDtos.AnimalResponse> update(@PathVariable Long id, @Valid @RequestBody AnimalDtos.SaveAnimalRequest request) {
        return ApiResponse.ok(animalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        animalService.offline(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AnimalDtos.AnimalResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AnimalDtos.UpdateAnimalStatusRequest request
    ) {
        return ApiResponse.ok(animalService.updateStatus(id, request));
    }
}
