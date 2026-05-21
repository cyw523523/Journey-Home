package com.guitu.service;

import com.guitu.domain.CommunityCommentMention;
import com.guitu.domain.User;
import com.guitu.dto.CommunityDtos.MentionInfo;
import com.guitu.repository.CommunityCommentMentionRepository;
import com.guitu.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityMentionParserTest {
    @Mock
    UserRepository userRepo;

    @Mock
    CommunityCommentMentionRepository mentionRepo;

    @InjectMocks
    CommunityMentionParser parser;

    @Test
    void shouldParseSingleMention() {
        User alice = new User();
        alice.setId(1L);
        alice.setNickname("小明");
        when(userRepo.findByNicknameIn(List.of("小明"))).thenReturn(List.of(alice));
        when(mentionRepo.save(any())).thenReturn(new CommunityCommentMention());

        List<MentionInfo> result = parser.parse("谢谢你 @小明 分享", 1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nickname()).isEqualTo("小明");
        assertThat(result.get(0).userId()).isEqualTo(1L);
    }

    @Test
    void shouldHandleUnmatchedNickname() {
        when(userRepo.findByNicknameIn(List.of("不存在的用户"))).thenReturn(List.of());
        List<MentionInfo> result = parser.parse("@不存在的用户 你好", 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleMultipleMentions() {
        User alice = new User();
        alice.setId(1L);
        alice.setNickname("小明");
        User bob = new User();
        bob.setId(2L);
        bob.setNickname("小红");
        when(userRepo.findByNicknameIn(List.of("小明", "小红"))).thenReturn(List.of(alice, bob));
        when(mentionRepo.save(any())).thenReturn(new CommunityCommentMention());

        List<MentionInfo> result = parser.parse("@小明 @小红 谢谢", 1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldIgnoreEscapedAt() {
        List<MentionInfo> result = parser.parse("@@ 不是提及", 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNoMention() {
        List<MentionInfo> result = parser.parse("普通评论没有提及", 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleNullContent() {
        List<MentionInfo> result = parser.parse(null, 1L);
        assertThat(result).isEmpty();
    }
}
