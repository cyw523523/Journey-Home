package com.guitu.repository;

import com.guitu.domain.Notice;
import com.guitu.domain.enums.NoticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {
    long countByStatus(NoticeStatus status);
}
