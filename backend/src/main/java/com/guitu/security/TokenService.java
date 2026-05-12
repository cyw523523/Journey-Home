package com.guitu.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.security.token-secret}")
    private String secret;

    @Value("${app.security.token-ttl-hours:168}")
    private long ttlHours;

    public String generate(Long userId) {
        long expiresAt = Instant.now().plusSeconds(ttlHours * 3600).toEpochMilli();
        String payload = userId + ":" + expiresAt;
        String signature = sign(payload);
        String raw = payload + "." + signature;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Long> parseUserId(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int index = raw.lastIndexOf('.');
            if (index <= 0) {
                return Optional.empty();
            }
            String payload = raw.substring(0, index);
            String signature = raw.substring(index + 1);
            if (!sign(payload).equals(signature)) {
                return Optional.empty();
            }
            String[] parts = payload.split(":");
            if (parts.length != 2) {
                return Optional.empty();
            }
            long expiresAt = Long.parseLong(parts[1]);
            if (expiresAt < Instant.now().toEpochMilli()) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(parts[0]));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Token 签名失败", ex);
        }
    }
}
