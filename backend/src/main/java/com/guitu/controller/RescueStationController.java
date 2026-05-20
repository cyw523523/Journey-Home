package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.CertificationStatus;
import com.guitu.dto.RescueStationDtos;
import com.guitu.security.SecuritySupport;
import com.guitu.service.RescueStationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rescue-stations")
public class RescueStationController {
    private final RescueStationService rescueStationService;

    public RescueStationController(RescueStationService rescueStationService) {
        this.rescueStationService = rescueStationService;
    }

    @PostMapping("/apply")
    public ApiResponse<RescueStationDtos.StationResponse> apply(@Valid @RequestBody RescueStationDtos.ApplyRequest request) {
        return ApiResponse.ok(rescueStationService.apply(request));
    }

    @GetMapping("/me")
    public ApiResponse<RescueStationDtos.StationResponse> myStation() {
        return ApiResponse.ok(rescueStationService.getMyStation());
    }

    @PutMapping("/me")
    public ApiResponse<RescueStationDtos.StationResponse> updateProfile(@Valid @RequestBody RescueStationDtos.UpdateProfileRequest request) {
        return ApiResponse.ok(rescueStationService.updateProfile(request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<RescueStationDtos.StationResponse> publicStation(@PathVariable Long userId) {
        return ApiResponse.ok(rescueStationService.getPublicStation(userId));
    }

    @PostMapping("/{stationUserId}/follow")
    public ApiResponse<Void> follow(@PathVariable Long stationUserId) {
        rescueStationService.follow(stationUserId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{stationUserId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long stationUserId) {
        rescueStationService.unfollow(stationUserId);
        return ApiResponse.ok();
    }

    @GetMapping("/{stationUserId}/is-following")
    public ApiResponse<Boolean> isFollowing(@PathVariable Long stationUserId) {
        return ApiResponse.ok(rescueStationService.isFollowing(stationUserId));
    }

    @GetMapping("/me/followers")
    public ApiResponse<PageResponse<RescueStationDtos.FollowResponse>> followers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(rescueStationService.listMyFollowers(page, size));
    }

    @GetMapping("/me/following")
    public ApiResponse<PageResponse<RescueStationDtos.FollowResponse>> following(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(rescueStationService.listFollowingStations(page, size));
    }

    @GetMapping("/me/dashboard")
    public ApiResponse<RescueStationDtos.DashboardResponse> dashboard() {
        Long userId = com.guitu.security.SecuritySupport.requireUser().id();
        return ApiResponse.ok(rescueStationService.dashboard(userId));
    }

    @GetMapping("/discover")
    public ApiResponse<PageResponse<RescueStationDtos.StationResponse>> discover(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(rescueStationService.listApproved(page, size));
    }

    // ========== Admin endpoints ==========

    @GetMapping("/admin/list")
    public ApiResponse<PageResponse<RescueStationDtos.StationResponse>> adminList(
            @RequestParam(required = false) CertificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SecuritySupport.requireAdmin();
        return ApiResponse.ok(rescueStationService.listAll(status, page, size));
    }

    @PostMapping("/admin/{userId}/certify")
    public ApiResponse<RescueStationDtos.StationResponse> certify(
            @PathVariable Long userId,
            @RequestBody java.util.Map<String, Object> body
    ) {
        SecuritySupport.requireAdmin();
        String statusStr = (String) body.get("status");
        String reason = (String) body.get("reason");
        com.guitu.domain.enums.CertificationStatus status = com.guitu.domain.enums.CertificationStatus.valueOf(statusStr);
        return ApiResponse.ok(rescueStationService.adminCertify(userId, status, reason));
    }
}
