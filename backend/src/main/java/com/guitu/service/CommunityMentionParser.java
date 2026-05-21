package com.guitu.service;

import com.guitu.domain.CommunityCommentMention;
import com.guitu.domain.User;
import com.guitu.repository.CommunityCommentMentionRepository;
import com.guitu.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Service
public class CommunityMentionParser {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s@]{1,20})");
    private final UserRepository userRepo;
    private final CommunityCommentMentionRepository mentionRepo;

    public CommunityMentionParser(UserRepository userRepo, CommunityCommentMentionRepository mentionRepo) {
        this.userRepo = userRepo;
        this.mentionRepo = mentionRepo;
    }

    public List<Map<String, Object>> parse(String content, Long commentId) {
        if (content == null) return List.of();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> candidateNicks = new LinkedHashSet<>();
        while (matcher.find()) candidateNicks.add(matcher.group(1));
        if (candidateNicks.isEmpty()) return List.of();

        List<User> matched = userRepo.findByNicknameIn(new ArrayList<>(candidateNicks));
        Map<String, User> nickToUser = new HashMap<>();
        for (User u : matched) nickToUser.put(u.getNickname(), u);

        List<Map<String, Object>> result = new ArrayList<>();
        for (String nick : candidateNicks) {
            User u = nickToUser.get(nick);
            if (u != null) {
                CommunityCommentMention m = new CommunityCommentMention();
                m.setCommentId(commentId);
                m.setMentionedUserId(u.getId());
                mentionRepo.save(m);
                Map<String, Object> info = new HashMap<>();
                info.put("userId", u.getId());
                info.put("nickname", u.getNickname());
                result.add(info);
            }
        }
        return result;
    }
}
