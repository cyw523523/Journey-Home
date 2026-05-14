package com.guitu.service;

import com.guitu.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class ContentModerationService {
    private final List<String> blockedKeywords;
    private final List<String> reviewKeywords;

    public ContentModerationService(
            @Value("${app.moderation.blocked-keywords:涉黄,暴力,诈骗,赌博,毒品,约炮,兼职刷单}") String blockedKeywordsValue,
            @Value("${app.moderation.review-keywords:领养代办,微信联系,私聊交易,寄养收费,转账,二维码}") String reviewKeywordsValue
    ) {
        this.blockedKeywords = parseKeywords(blockedKeywordsValue);
        this.reviewKeywords = parseKeywords(reviewKeywordsValue);
    }

    public void validateText(String fieldName, String... values) {
        String keyword = findKeyword(blockedKeywords, values);
        if (keyword != null) {
            throw new BusinessException(fieldName + " contains blocked content: " + keyword);
        }
    }

    public boolean requiresManualReview(List<String> imageUrls, String... values) {
        return (imageUrls != null && !imageUrls.isEmpty()) || findKeyword(reviewKeywords, values) != null;
    }

    private String findKeyword(List<String> keywords, String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (normalized.contains(keyword)) {
                    return keyword;
                }
            }
        }
        return null;
    }

    private List<String> parseKeywords(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> item.toLowerCase(Locale.ROOT))
                .toList();
    }
}
