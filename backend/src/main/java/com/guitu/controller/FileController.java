package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.FileDtos;
import com.guitu.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ApiResponse<FileDtos.UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "common") String usage
    ) {
        return ApiResponse.ok(fileStorageService.upload(file, usage));
    }
}
