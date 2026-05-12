package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.dto.AnimalDtos;
import com.guitu.dto.RescueDtos;
import com.guitu.dto.UserDtos;
import com.guitu.service.AdoptApplyService;
import com.guitu.service.AnimalService;
import com.guitu.service.RescueService;
import com.guitu.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService userService;
    private final AnimalService animalService;
    private final RescueService rescueService;
    private final AdoptApplyService adoptApplyService;

    public UserController(
            UserService userService,
            AnimalService animalService,
            RescueService rescueService,
            AdoptApplyService adoptApplyService
    ) {
        this.userService = userService;
        this.animalService = animalService;
        this.rescueService = rescueService;
        this.adoptApplyService = adoptApplyService;
    }

    @GetMapping
    public ApiResponse<UserDtos.UserProfile> profile() {
        return ApiResponse.ok(userService.currentProfile());
    }

    @PutMapping
    public ApiResponse<UserDtos.UserProfile> updateProfile(@Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateCurrent(request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.ok();
    }

    @GetMapping("/animals")
    public ApiResponse<PageResponse<AnimalDtos.AnimalResponse>> myAnimals(
            @RequestParam(required = false) AnimalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(animalService.listMine(status, page, size));
    }

    @GetMapping("/rescues")
    public ApiResponse<PageResponse<RescueDtos.RescueResponse>> myRescues(
            @RequestParam(required = false) RescueStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(rescueService.listMine(status, page, size));
    }

    @GetMapping("/applications")
    public ApiResponse<PageResponse<AdoptApplyDtos.ApplyResponse>> myApplications(
            @RequestParam(required = false) ApplyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(adoptApplyService.listMine(status, page, size));
    }
}
