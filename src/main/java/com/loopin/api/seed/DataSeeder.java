package com.loopin.api.seed;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.*;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupJoinRequest;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.GroupJoinRequestRepository;
import com.loopin.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventGroupRepository eventGroupRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;

    // Constructor Injection
    public DataSeeder(UserRepository userRepository,
                      EventRepository eventRepository,
                      EventGroupRepository eventGroupRepository,
                      GroupJoinRequestRepository groupJoinRequestRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventGroupRepository = eventGroupRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // avoid duplicates
        groupJoinRequestRepository.deleteAll();
        eventGroupRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        System.out.println("🌱 Demo data seeding started...");

        // ==========================================
        // 1. Users (1 Admin + 5 User)
        // ==========================================
        User admin = new User();
        admin.setName("System Admin");
        admin.setEmail("admin@loopin.com");
        admin.setGoogleId("google_admin_123");
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        userRepository.save(admin);

        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setName("Demo User " + i);
            user.setEmail("user" + i + "@loopin.com");
            user.setGoogleId("google_user_" + i);
            user.setRole(Role.USER);
            user.setIsActive(true);
            users.add(userRepository.save(user));
        }

        // ==========================================
        // 2. EVENTS (8 Event)
        // ==========================================
        List<Event> events = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Event event = new Event();
            event.setTitle("Loopin Tech Event " + i);
            event.setDescription("This is a detailed description for loopin demo event number " + i);


            event.setType(i % 2 == 0 ? EventType.ACTIVITY : EventType.EVENT);
            event.setCategory(EventCategory.TECH);
            event.setCity("Baku");
            event.setAddress("Nizami Str. " + i);
            event.setStartDateTime(LocalDateTime.now().plusDays(i));
            event.setEndDateTime(LocalDateTime.now().plusDays(i).plusHours(3));
            event.setIsFree(i % 2 == 0);
            event.setPrice(i % 2 == 0 ? BigDecimal.ZERO : new BigDecimal("25.00"));
            event.setOrganizerName("Loopin Organization");
            event.setImageUrl("https://images.loopin.com/event" + i + ".png");
            event.setStatus(EventStatus.PUBLISHED);
            event.setOwner(users.get(i % 5));

            events.add(eventRepository.save(event));
        }

        // ==========================================
        // 3. GROUPS (8 EventGroup)
        // ==========================================
        List<EventGroup> groups = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            EventGroup group = new EventGroup();
            group.setEvent(events.get(i - 1));
            group.setAdmin(users.get(i % 5));  
            group.setTitle("Discussion Group for Event " + i);
            group.setGroupSize(GroupSizeType.FOUR);
            group.setMaxMembers(20);
            group.setStatus(GroupStatus.OPEN);
            group.setGroupNote("Welcome to the group! Let's discuss about the event.");

            groups.add(eventGroupRepository.save(group));
        }

        // ==========================================
        // 4. REQUESTS (10 GroupJoinRequest)
        // ==========================================
        for (int i = 1; i <= 10; i++) {
            GroupJoinRequest request = new GroupJoinRequest();
            request.setGroup(groups.get(i % 8));
            request.setUser(users.get(i % 5));
            request.setStatus(RequestStatus.PENDING);
            request.setMessage("Hello, I would like to join this group for collaboration. Request #" + i);

            groupJoinRequestRepository.save(request);
        }

        System.out.println("✅ Seeding completed successfully!");
        System.out.println("📊 Summary: 1 Admin, 5 Users, 8 Events, 8 Groups, 10 Requests inserted.");
    }
}
