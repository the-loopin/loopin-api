package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loopin.api.common.security.JwtUtils;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventGroupRepository eventGroupRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        eventGroupRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("admin@email.com", "Admin User", null);
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        regularUser = new User("user@email.com", "Regular User", null);
        regularUser.setRole(Role.USER);
        regularUser = userRepository.save(regularUser);

        adminToken = jwtUtils.generateToken(adminUser.getEmail(), Role.ADMIN.name());
        userToken = jwtUtils.generateToken(regularUser.getEmail(), Role.USER.name());
    }

    @Test
    void getDashboardStats_Success() throws Exception {
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
        eventRepository.save(event);

        mockMvc.perform(get("/admin/dashboard/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", is(2)))
                .andExpect(jsonPath("$.activeEvents", is(1)))
                .andExpect(jsonPath("$.totalGroups", is(0)));
    }

    @Test
    void getUsers_Paged_Success() throws Exception {
        mockMvc.perform(get("/admin/users?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void updateUserRole_Success() throws Exception {
        User secondAdmin = new User("admin2@email.com", "Admin Two", null);
        secondAdmin.setRole(Role.ADMIN);
        userRepository.save(secondAdmin);

        mockMvc.perform(put("/admin/users/" + regularUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    void updateUserRole_SelfDemotion_Forbidden() throws Exception {
        mockMvc.perform(put("/admin/users/" + adminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserRole_LastAdminDemotion_BadRequest() throws Exception {
        User otherAdmin = new User("anotherAdmin@email.com", "Other Admin", null);
        otherAdmin.setRole(Role.ADMIN);
        otherAdmin.setIsActive(false);
        userRepository.save(otherAdmin);

        String otherAdminToken = jwtUtils.generateToken("anotherAdmin@email.com", Role.ADMIN.name());
        mockMvc.perform(put("/admin/users/" + adminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + otherAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserRole_InvalidRole_BadRequest() throws Exception {
        mockMvc.perform(put("/admin/users/" + regularUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"INVALID_ROLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_SoftDeletesUser() throws Exception {
        mockMvc.perform(delete("/admin/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        User deleted = userRepository.findById(regularUser.getId()).orElseThrow();
        assertFalse(deleted.getIsActive());
        assertTrue(deleted.isDeleted());
    }

    @Test
    void deleteUser_SelfDelete_Forbidden() throws Exception {
        mockMvc.perform(delete("/admin/users/" + adminUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_LastAdmin_BadRequest() throws Exception {
        User otherAdmin = new User("anotherAdmin@email.com", "Other Admin", null);
        otherAdmin.setRole(Role.ADMIN);
        otherAdmin.setIsActive(false);
        userRepository.save(otherAdmin);

        String otherAdminToken = jwtUtils.generateToken("anotherAdmin@email.com", Role.ADMIN.name());
        mockMvc.perform(delete("/admin/users/" + adminUser.getId())
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_FilterByStatus_Success() throws Exception {
        Event event = new Event();
        event.setTitle("Published Event");
        event.setDescription("Desc");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Baku");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(2));
        event.setIsFree(true);
        event.setOrganizerName("Org");
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);

        mockMvc.perform(get("/admin/events?status=PUBLISHED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void deleteEvent_TransitionsToCancelled() throws Exception {
        Event event = new Event();
        event.setTitle("Event to Cancel");
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

        mockMvc.perform(delete("/admin/events/" + event.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Event cancelledEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(EventStatus.CANCELLED, cancelledEvent.getStatus());
    }

    @Test
    void adminEndpoints_UserRole_Forbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
