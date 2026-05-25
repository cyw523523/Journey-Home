package com.guitu.repository;

import com.guitu.domain.UserLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLocationHistoryRepository extends JpaRepository<UserLocationHistory, Long> {
}
