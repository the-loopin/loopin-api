package com.loopin.api.seed;


import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "loopin.seed.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserSeeder userSeeder;
    private final EventSeeder eventSeeder;
    private final EventGroupSeeder eventGroupSeeder;
    private final GroupJoinRequestSeeder groupJoinRequestSeeder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🚀 Starting database seeding process...");

        // 1. Seed Users (1 Admin, 5 Regular Users)
        List<User> savedUsers = userSeeder.seedUsers();
        log.info("Successfully checked/seeded {} users.", savedUsers.size());

        // 2. Seed 8 Events
        List<Event> savedEvents = eventSeeder.seedEvents(savedUsers);
        log.info("Successfully checked/seeded {} events.", savedEvents.size());

        // 3. Seed 8 Groups
        List<EventGroup> savedGroups = eventGroupSeeder.seedGroups(savedEvents, savedUsers);
        log.info("Successfully checked/seeded {} event groups.", savedGroups.size());

        // 4. Seed 10 Requests
        groupJoinRequestSeeder.seedRequests(savedGroups, savedUsers);
        log.info("Successfully checked/seeded 10 group join requests.");

        log.info("✅ Database seeding complete!");
    }
}
