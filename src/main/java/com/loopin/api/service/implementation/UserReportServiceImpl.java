package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.ReportStatus;
import com.loopin.api.common.enums.ReportTargetType;
import com.loopin.api.common.enums.NotificationReferenceType;
import com.loopin.api.common.enums.NotificationType;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.report.request.CreateReportRequest;
import com.loopin.api.dto.report.response.ReportResponse;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMessage;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserReport;
import com.loopin.api.mapper.UserReportMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.GroupMessageRepository;
import com.loopin.api.repository.UserReportRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.UserReportService;
import com.loopin.api.service.abstraction.NotificationService;
import com.loopin.api.service.notification.NotificationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReportServiceImpl implements UserReportService {

    private final UserReportRepository reportRepository;
    private final UserRepository userRepository;
    private final EventGroupRepository eventGroupRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserReportMapper reportMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ReportResponse create(Long reporterId, CreateReportRequest request) {
        User reporter = userRepository.findByIdAndDeletedAtIsNull(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reporterId));

        UserReport report = new UserReport();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());
        report.setStatus(ReportStatus.PENDING);

        assignTarget(report, request);

        return reportMapper.toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<UserReport> reports = status == null
                ? reportRepository.findAllBy(pageable)
                : reportRepository.findByStatus(status, pageable);
        return reports.map(reportMapper::toResponse);
    }

    @Override
    @Transactional
    public ReportResponse updateStatus(UUID reportId, ReportStatus status) {
        UserReport report = reportRepository.findByPublicId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        ReportStatus previousStatus = report.getStatus();
        report.setStatus(status);
        UserReport saved = reportRepository.save(report);
        if (previousStatus != status) {
            notificationService.create(new NotificationCommand(
                    report.getReporter(),
                    NotificationType.MODERATION_UPDATE,
                    "Report status updated",
                    "Your report status is now " + status.name().toLowerCase() + ".",
                    NotificationReferenceType.REPORT,
                    report.getPublicId()));
        }
        return reportMapper.toResponse(saved);
    }

    private void assignTarget(UserReport report, CreateReportRequest request) {
        if (request.getTargetType() == ReportTargetType.GROUP) {
            EventGroup group = eventGroupRepository.findByPublicId(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + request.getTargetId()));
            report.setGroup(group);
            return;
        }

        GroupMessage message = groupMessageRepository.findByPublicId(request.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + request.getTargetId()));
        report.setMessage(message);
    }
}
