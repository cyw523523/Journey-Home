package com.guitu.service;

import com.guitu.dto.FileDtos;
import com.guitu.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.public-path:/uploads}")
    private String publicPath;

    public FileDtos.UploadResponse upload(MultipartFile file, String usage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }

        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("图片格式仅支持 JPG、JPEG、PNG、WEBP");
        }

        String safeUsage = usage == null || usage.isBlank() ? "common" : usage.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        String datePath = LocalDate.now().toString().replace("-", "/");
        String filename = UUID.randomUUID() + "." + extension;
        Path dir = Path.of(uploadDir, safeUsage, datePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException("图片上传失败，请稍后重试");
        }

        String url = publicPath + "/" + safeUsage + "/" + datePath + "/" + filename;
        return new FileDtos.UploadResponse(url, original, file.getSize());
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
