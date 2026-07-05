package com.loopin.api.repository;

import com.loopin.api.common.enums.ReportStatus;
import com.loopin.api.entity.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    @EntityGraph(attributePaths = {"reporter"})
    Page<UserReport> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter"})
    Page<UserReport> findAllBy(Pageable pageable);

    Optional<UserReport> findByPublicId(UUID publicId);
}
