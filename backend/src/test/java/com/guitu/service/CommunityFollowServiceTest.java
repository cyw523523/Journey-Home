package com.guitu.service;

import com.guitu.domain.CommunityUserFollow;
import com.guitu.exception.BusinessException;
import com.guitu.repository.CommunityUserFollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityFollowServiceTest {
    @Mock
    CommunityUserFollowRepository repo;

    @InjectMocks
    CommunityFollowService service;

    @Test
    void shouldRejectSelfFollow() {
        assertThatThrownBy(() -> service.follow(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能关注自己");
    }

    @Test
    void shouldBeIdempotentOnFollow() {
        when(repo.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(true);
        service.follow(1L, 2L);
        verify(repo, never()).save(any());
    }

    @Test
    void shouldSaveOnFirstFollow() {
        when(repo.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(false);
        when(repo.save(any())).thenReturn(new CommunityUserFollow());
        service.follow(1L, 2L);
        verify(repo).save(any());
    }

    @Test
    void shouldBeIdempotentOnUnfollow() {
        when(repo.findByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(Optional.empty());
        service.unfollow(1L, 2L);
        verify(repo, never()).delete(any());
    }

    @Test
    void shouldDeleteOnUnfollow() {
        CommunityUserFollow existing = new CommunityUserFollow();
        when(repo.findByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(Optional.of(existing));
        service.unfollow(1L, 2L);
        verify(repo).delete(existing);
    }
}
