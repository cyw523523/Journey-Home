package com.guitu.repository;

import com.guitu.domain.ContentReport;
import com.guitu.domain.enums.ReportStatus;
import com.guitu.domain.enums.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long>, JpaSpecificationExecutor<ContentReport> {
    boolean existsByTargetTypeAndTargetIdAndReporterIdAndStatusIn(
            ReportTargetType targetType,
            Long targetId,
            Long reporterId,
            java.util.Collection<ReportStatus> statuses
    );
}
