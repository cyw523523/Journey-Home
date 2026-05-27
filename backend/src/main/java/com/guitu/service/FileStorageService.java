package com.guitu.service;

import com.guitu.dto.FileDtos;
import com.guitu.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

    public String saveBytes(byte[] bytes, String usage, String filename) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("保存文件内容不能为空");
        }
        if (filename == null || filename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
        return saveStream(new java.io.ByteArrayInputStream(bytes), usage, filename);
    }

    public String saveStream(InputStream inputStream, String usage, String filename) {
        String safeUsage = usage == null || usage.isBlank() ? "common" : usage.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String datePath = LocalDate.now().toString().replace("-", "/");
        Path dir = Path.of(uploadDir, safeUsage, datePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(safeFilename).normalize();
            Files.copy(inputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败，请稍后重试");
        }
        return publicPath + "/" + safeUsage + "/" + datePath + "/" + safeFilename;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
