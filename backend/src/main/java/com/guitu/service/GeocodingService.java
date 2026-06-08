package com.guitu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeocodingService {
    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String AMAP_GEO_URL = "https://restapi.amap.com/v3/geocode/geo";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeocodingService(
            @Value("${app.amap.geocode-key:}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public record GeoResult(Double longitude, Double latitude) {}

    /**
     * 将地址文本转换为经纬度坐标。如果没有配置 API key，返回 null。
     */
    public GeoResult geocode(String address) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AMap geocode key not configured, skipping geocoding for: {}", address);
            return null;
        }
        if (address == null || address.isBlank()) {
            return null;
        }
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            URI uri = URI.create(AMAP_GEO_URL + "?key=" + apiKey + "&address=" + encoded + "&output=JSON");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Geocoding API returned status {} for address: {}", response.statusCode(), address);
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Geocoding failed for address '{}': {}", address, root.path("info").asText());
                return null;
            }
            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isEmpty()) {
                log.warn("No geocode result for address: {}", address);
                return null;
            }
            String location = geocodes.get(0).path("location").asText();
            String[] parts = location.split(",");
            if (parts.length == 2) {
                double lng = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                return new GeoResult(lng, lat);
            }
        } catch (Exception e) {
            log.warn("Geocoding error for address '{}': {}", address, e.getMessage());
        }
        return null;
    }
}
