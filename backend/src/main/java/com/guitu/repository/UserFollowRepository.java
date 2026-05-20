package com.guitu.repository;

import com.guitu.domain.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Page<UserFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId, Pageable pageable);

    @Query("SELECT f FROM UserFollow f WHERE f.stationUser.id = :stationUserId ORDER BY f.createdAt DESC")
    Page<UserFollow> findByStationUserId(Long stationUserId, Pageable pageable);

    @Query("SELECT f FROM UserFollow f WHERE f.follower.id = :followerId AND f.stationUser.id = :stationUserId")
    Optional<UserFollow> findByFollowerAndStation(Long followerId, Long stationUserId);

    long countByStationUser_Id(Long stationUserId);
}
