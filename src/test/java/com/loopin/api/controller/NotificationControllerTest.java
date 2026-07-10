package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.notifications.enums.NotificationStatus;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.notifications.entity.Notification;
import com.loopin.api.users.entity.User;
import com.loopin.api.notifications.repository.NotificationDeliveryRepository;
import com.loopin.api.notifications.repository.NotificationRepository;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User owner;
    private User otherUser;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        owner = saveUser("owner-notifications@test.com", "Owner");
        otherUser = saveUser("other-notifications@test.com", "Other");
        ownerToken = jwtUtils.generateToken(owner.getEmail(), owner.getRole().name());
    }

    @Test
    void list_ReturnsOnlyAuthenticatedUsersNotifications() throws Exception {
        saveNotification(owner, "Mine");
        saveNotification(otherUser, "Not mine");

        mockMvc.perform(get("/v1/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Mine"));
    }

    @Test
    void readAndArchive_EnforceOwnershipAndStatusTransitions() throws Exception {
        Notification mine = saveNotification(owner, "Mine");
        Notification theirs = saveNotification(otherUser, "Theirs");

        mockMvc.perform(patch("/v1/notifications/{id}/read", theirs.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/v1/notifications/{id}/read", mine.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));

        mockMvc.perform(delete("/v1/notifications/{id}", mine.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/v1/notifications")
                        .param("status", NotificationStatus.ARCHIVED.name())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void markAllRead_UpdatesOnlyCurrentUsersUnreadNotifications() throws Exception {
        saveNotification(owner, "First");
        saveNotification(owner, "Second");
        saveNotification(otherUser, "Other");

        mockMvc.perform(patch("/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/notifications")
                        .param("status", NotificationStatus.UNREAD.name())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private User saveUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(Role.USER);
        user.setIsActive(true);
        return userRepository.saveAndFlush(user);
    }

    private Notification saveNotification(User recipient, String title) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.EVENT_UPDATE);
        notification.setTitle(title);
        notification.setMessage("Message");
        return notificationRepository.saveAndFlush(notification);
    }
}
