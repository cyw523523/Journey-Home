package com.guitu.repository;

import com.guitu.domain.User;
import com.guitu.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByAccount(String account);

    boolean existsByAccount(String account);

    boolean existsByPhone(String phone);

    long countByStatus(UserStatus status);

    List<User> findByNicknameIn(List<String> nicknames);

    Page<User> findByNicknameStartingWith(String prefix, Pageable pageable);
}
