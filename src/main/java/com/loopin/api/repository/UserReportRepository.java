package com.loopin.api.repository;

import com.loopin.api.common.enums.ReportStatus;
import com.loopin.api.entity.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    Page<UserReport> findByStatus(ReportStatus status, Pageable pageable);
}
