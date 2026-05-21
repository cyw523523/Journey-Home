package com.guitu.service;

import com.guitu.domain.CommunityPost;
import com.guitu.domain.CommunityPostFavorite;
import com.guitu.domain.User;
import com.guitu.repository.CommunityPostFavoriteRepository;
import com.guitu.repository.CommunityPostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityFavoriteService {
    private final CommunityPostFavoriteRepository repo;
    private final CommunityPostRepository postRepo;

    public CommunityFavoriteService(CommunityPostFavoriteRepository repo, CommunityPostRepository postRepo) {
        this.repo = repo; this.postRepo = postRepo;
    }

    @Transactional
    public boolean toggle(Long userId, Long postId) {
        return repo.findByUserIdAndPostId(userId, postId)
            .map(fav -> { repo.delete(fav); updateCount(postId, -1); return false; })
            .orElseGet(() -> {
                CommunityPostFavorite fav = new CommunityPostFavorite();
                User u = new User(); u.setId(userId); fav.setUser(u);
                CommunityPost p = new CommunityPost(); p.setId(postId); fav.setPost(p);
                repo.save(fav);
                updateCount(postId, 1);
                return true;
            });
    }

    public boolean isFavorited(Long userId, Long postId) {
        return repo.existsByUserIdAndPostId(userId, postId);
    }

    public Page<CommunityPostFavorite> listMine(Long userId, Pageable pageable) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    private void updateCount(Long postId, int delta) {
        postRepo.findById(postId).ifPresent(p -> { p.setFavoriteCount(p.getFavoriteCount() + delta); postRepo.save(p); });
    }
}
