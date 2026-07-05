package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.common.enums.GroupSizeType;
import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.common.enums.ReportStatus;
import com.loopin.api.common.enums.ReportTargetType;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserReport;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.repository.UserReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventGroupRepository eventGroupRepository;

    @Autowired
    private UserReportRepository reportRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User adminUser;
    private User regularUser;
    private EventGroup group;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        eventGroupRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("admin@email.com", "Admin User", null);
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        regularUser = new User("user@email.com", "Regular User", null);
        regularUser.setRole(Role.USER);
        regularUser = userRepository.save(regularUser);

        Event event = new Event();
        event.setTitle("Test Event");
        event.setDescription("Desc");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Baku");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(2));
        event.setIsFree(true);
        event.setOrganizerName("Org");
        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);

        group = new EventGroup();
        group.setEvent(event);
        group.setAdmin(adminUser);
        group.setTitle("Reportable Group");
        group.setGroupSize(GroupSizeType.FOUR);
        group.setMaxMembers(4);
        group.setStatus(GroupStatus.OPEN);
        group = eventGroupRepository.save(group);

        adminToken = jwtUtils.generateToken(adminUser.getEmail(), Role.ADMIN.name());
        userToken = jwtUtils.generateToken(regularUser.getEmail(), Role.USER.name());
    }

    @Test
    void createReport_LoggedInUser_SavesPendingReport() throws Exception {
        mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "GROUP",
                                  "targetId": %d,
                                  "reason": "Spam",
                                  "details": "This group is advertising unrelated links."
                                }
                                """.formatted(group.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporterId", is(regularUser.getId().intValue())))
                .andExpect(jsonPath("$.targetType", is("GROUP")))
                .andExpect(jsonPath("$.targetId", is(group.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")));

        UserReport report = reportRepository.findAll().getFirst();
        assertEquals(regularUser.getId(), report.getReporter().getId());
        assertEquals(ReportTargetType.GROUP, report.getTargetType());
        assertEquals(group.getId(), report.getGroup().getId());
        assertEquals(ReportStatus.PENDING, report.getStatus());
    }

    @Test
    void createReport_AnonymousUser_Unauthorized() throws Exception {
        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "GROUP",
                                  "targetId": %d,
                                  "reason": "Spam"
                                }
                                """.formatted(group.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReports_AdminOnlyAndCanFilterPending() throws Exception {
        UserReport report = pendingGroupReport();

        mockMvc.perform(get("/admin/reports?status=PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(report.getId().intValue())))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReport_AdminCanUpdateStatus() throws Exception {
        UserReport report = pendingGroupReport();

        mockMvc.perform(patch("/admin/reports/" + report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));

        assertEquals(ReportStatus.RESOLVED, reportRepository.findById(report.getId()).orElseThrow().getStatus());
    }

    private UserReport pendingGroupReport() {
        UserReport report = new UserReport();
        report.setReporter(regularUser);
        report.setTargetType(ReportTargetType.GROUP);
        report.setGroup(group);
        report.setReason("Spam");
        report.setStatus(ReportStatus.PENDING);
        return reportRepository.save(report);
    }
}
