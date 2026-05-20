package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.DonationStatus;
import com.guitu.domain.enums.SupplyCategory;
import com.guitu.dto.DonationDtos;
import com.guitu.service.DonationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
public class DonationController {
    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DonationDtos.SupplyDemandResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SupplyCategory category,
            @RequestParam(required = false) DonationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(donationService.listPublic(keyword, category, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DonationDtos.SupplyDemandResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(donationService.detail(id));
    }

    @GetMapping("/{id}/records")
    public ApiResponse<List<DonationDtos.DonationRecordResponse>> records(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(donationService.listRecordsByDemand(id, page, size));
    }

    @PostMapping
    public ApiResponse<DonationDtos.SupplyDemandResponse> create(@Valid @RequestBody DonationDtos.SaveSupplyDemandRequest request) {
        return ApiResponse.ok(donationService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DonationDtos.SupplyDemandResponse> update(@PathVariable Long id, @Valid @RequestBody DonationDtos.SaveSupplyDemandRequest request) {
        return ApiResponse.ok(donationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        donationService.offline(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/donate")
    public ApiResponse<DonationDtos.DonationRecordResponse> donate(@PathVariable Long id, @Valid @RequestBody DonationDtos.DonateRequest request) {
        return ApiResponse.ok(donationService.donate(id, request));
    }

    @PatchMapping("/records/{recordId}/complete")
    public ApiResponse<DonationDtos.DonationRecordResponse> completeDonation(@PathVariable Long recordId) {
        return ApiResponse.ok(donationService.completeDonation(recordId));
    }
}
