package com.loopin.api.reports.mapper;

import com.loopin.api.reports.enums.ReportStatus;
import com.loopin.api.reports.enums.ReportTargetType;
import com.loopin.api.reports.dto.response.ReportResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.chat.entity.GroupMessage;
import com.loopin.api.users.entity.User;
import com.loopin.api.reports.entity.UserReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserReportMapperTest {

    private UserReportMapper userReportMapper;

    @BeforeEach
    void setUp() {
        userReportMapper = new UserReportMapper();
    }

    @Test
    void toResponse_GroupTarget_MapsFieldsAndUsesPublicId() {
        UserReport report = new UserReport();
        report.setId(444L); // Internal ID
        UUID reportPublicId = UUID.randomUUID();
        report.setPublicId(reportPublicId);

        User reporter = new User();
        UUID reporterPublicId = UUID.randomUUID();
        reporter.setPublicId(reporterPublicId);
        report.setReporter(reporter);

        report.setTargetType(ReportTargetType.GROUP);
        
        EventGroup group = new EventGroup();
        UUID groupPublicId = UUID.randomUUID();
        group.setPublicId(groupPublicId);
        report.setGroup(group);

        report.setReason("Inappropriate behavior");
        report.setDetails("More details here");
        report.setStatus(ReportStatus.PENDING);
        
        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        ReportResponse response = userReportMapper.toResponse(report);

        assertEquals(reportPublicId, response.getId()); // EXPLICIT CHECK: Uses publicId
        assertEquals(reporterPublicId, response.getReporterId()); // EXPLICIT CHECK: Uses publicId
        assertEquals(ReportTargetType.GROUP, response.getTargetType());
        assertEquals(groupPublicId, response.getTargetId()); // EXPLICIT CHECK: Uses publicId
        assertEquals("Inappropriate behavior", response.getReason());
        assertEquals("More details here", response.getDetails());
        assertEquals(ReportStatus.PENDING, response.getStatus());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    void toResponse_MessageTarget_MapsFieldsAndUsesPublicId() {
        UserReport report = new UserReport();
        report.setId(444L);
        UUID reportPublicId = UUID.randomUUID();
        report.setPublicId(reportPublicId);

        User reporter = new User();
        UUID reporterPublicId = UUID.randomUUID();
        reporter.setPublicId(reporterPublicId);
        report.setReporter(reporter);

        report.setTargetType(ReportTargetType.MESSAGE);
        
        GroupMessage message = new GroupMessage();
        UUID messagePublicId = UUID.randomUUID();
        message.setPublicId(messagePublicId);
        report.setMessage(message);

        report.setReason("Spam");
        report.setStatus(ReportStatus.RESOLVED);

        ReportResponse response = userReportMapper.toResponse(report);

        assertEquals(reportPublicId, response.getId());
        assertEquals(reporterPublicId, response.getReporterId());
        assertEquals(ReportTargetType.MESSAGE, response.getTargetType());
        assertEquals(messagePublicId, response.getTargetId());
        assertEquals("Spam", response.getReason());
        assertEquals(ReportStatus.RESOLVED, response.getStatus());
    }

    @Test
    void toResponse_NullTarget_MapsFieldsWithNullTargetId() {
        UserReport report = new UserReport();
        UUID reportPublicId = UUID.randomUUID();
        report.setPublicId(reportPublicId);

        User reporter = new User();
        reporter.setPublicId(UUID.randomUUID());
        report.setReporter(reporter);

        report.setTargetType(null);
        report.setGroup(null);
        report.setMessage(null);

        ReportResponse response = userReportMapper.toResponse(report);

        assertEquals(reportPublicId, response.getId());
        assertNull(response.getTargetType());
        assertNull(response.getTargetId());
    }
}
