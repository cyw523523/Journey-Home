package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.MedicalRecordDtos;
import com.guitu.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/animals/{animalId}/medical-records")
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @GetMapping
    public ApiResponse<List<MedicalRecordDtos.MedicalRecordResponse>> list(@PathVariable Long animalId) {
        return ApiResponse.ok(medicalRecordService.listByAnimal(animalId));
    }

    @PostMapping
    public ApiResponse<MedicalRecordDtos.MedicalRecordResponse> create(
            @PathVariable Long animalId,
            @Valid @RequestBody MedicalRecordDtos.SaveMedicalRecordRequest request
    ) {
        return ApiResponse.ok(medicalRecordService.create(animalId, request));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<MedicalRecordDtos.MedicalRecordResponse> update(
            @PathVariable Long animalId,
            @PathVariable Long recordId,
            @Valid @RequestBody MedicalRecordDtos.SaveMedicalRecordRequest request
    ) {
        return ApiResponse.ok(medicalRecordService.update(recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(@PathVariable Long animalId, @PathVariable Long recordId) {
        medicalRecordService.delete(recordId);
        return ApiResponse.ok();
    }
}
