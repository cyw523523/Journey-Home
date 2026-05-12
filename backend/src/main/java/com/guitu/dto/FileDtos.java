package com.guitu.dto;

public final class FileDtos {
    private FileDtos() {
    }

    public record UploadResponse(String url, String originalFilename, long size) {
    }
}
