package com.guitu.repository;

import com.guitu.domain.AppealRecord;
import com.guitu.domain.enums.AppealStatus;
import com.guitu.domain.enums.AppealTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface AppealRecordRepository extends JpaRepository<AppealRecord, Long>, JpaSpecificationExecutor<AppealRecord> {
    boolean existsByTargetTypeAndTargetIdAndApplicantIdAndStatusIn(
            AppealTargetType targetType,
            Long targetId,
            Long applicantId,
            Collection<AppealStatus> statuses
    );
}
