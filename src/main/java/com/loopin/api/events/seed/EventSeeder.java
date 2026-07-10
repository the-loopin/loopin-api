package com.loopin.api.core.events.seed;


import com.loopin.api.core.events.enums.EventCategory;
import com.loopin.api.core.events.enums.EventStatus;
import com.loopin.api.core.events.enums.EventType;
import com.loopin.api.core.events.entity.Event;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.events.repository.EventRepository;
import com.loopin.api.core.users.seed.UserSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventSeeder {

    private final EventRepository eventRepository;
    private final UserSeeder userSeeder;

    public List<Event> seedEvents(List<User> users) {
        if (eventRepository.count() > 0) {
            return eventRepository.findAll();
        }

        // Own events with regular users only (an admin owning demo events
        // is not representative of real usage, and this also removes the
        // dependency on findAll() preserving insertion order).
        List<User> owners = userSeeder.getRegularUsers(users);
        if (owners.isEmpty()) {
            owners = users; // fallback, should not normally happen
        }

        List<Event> events = new ArrayList<>();

        // Structured realistic event data
        String[] titles = {
            "DevOps Loopin Summit",
            "Baku Founders Pitch Night",
            "Future of Work Panel",
            "AI & Education Seminar",
            "Shusha Heritage Tour",
            "Loopin Charity Marathon",
            "Baku Board Game Mixer",
            "Polyglot Coffee Exchange"
        };

        String[] descriptions = {
            "Join local DevOps engineers to discuss Kubernetes, Docker, and CI/CD pipelines in a modern cloud native world.",
            "Local startups present their ideas to active angel investors. Networking and pitching at its best.",
            "Discover modern strategies for hybrid team management, employee retention, and building a diverse company culture.",
            "Exploring how generative AI models are shaping modern learning frameworks, schools, and student resources.",
            "A guided travel experience exploring historical landmarks, nature trails, and cultural spots in Karabakh.",
            "Run for a cause! Participate in our annual 10K run around the Baku Boulevard to support local youth education.",
            "Meet new people, play chess, Catan, or ticket to ride, and enjoy local snacks in a cozy social environment.",
            "Practice your English, German, Russian, or Azerbaijani with native speakers over a warm cup of tea or coffee."
        };

        EventType[] types = {
            EventType.EVENT,
            EventType.EVENT,
            EventType.EVENT,
            EventType.EVENT,
            EventType.ACTIVITY,
            EventType.ACTIVITY,
            EventType.ACTIVITY,
            EventType.ACTIVITY
        };

        EventCategory[] categories = {
            EventCategory.TECH,
            EventCategory.STARTUP,
            EventCategory.HR,
            EventCategory.EDUCATION,
            EventCategory.TRAVEL,
            EventCategory.SPORT,
            EventCategory.SOCIAL,
            EventCategory.LANGUAGE
        };

        String[] cities = {
            "Baku",
            "Baku",
            "Ganja",
            "Sumqayit",
            "Shusha",
            "Baku",
            "Baku",
            "Ganja"
        };

        String[] addresses = {
            "Nizami Mall conference hall, 4th floor",
            "CBI Innovation Center, 5th Avenue",
            "Ganja Youth Center auditorium",
            "Sumqayit State University Lab Room B",
            "Jidir Duzu meeting point",
            "Baku Boulevard Clock Tower",
            "Loopin Social Club, Nizami Str. 21",
            "Coffee House Ganja, Atatürk Ave."
        };

        boolean[] isFrees = {
            true,
            false,
            true,
            false,
            false,
            false,
            true,
            true
        };

        BigDecimal[] prices = {
            BigDecimal.ZERO,
            new BigDecimal("15.00"),
            BigDecimal.ZERO,
            new BigDecimal("10.00"),
            new BigDecimal("50.00"),
            new BigDecimal("5.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        };

        String[] organizers = {
            "Baku Cloud Association",
            "Loopin Ventures",
            "HR Professionals Forum",
            "EduTech Labs",
            "Karabakh Travel Agency",
            "Loopin Hub Team",
            "Baku Gamers Guild",
            "Language Hub Baku"
        };

        for (int i = 0; i < 8; i++) {
            Event event = new Event();
            event.setTitle(titles[i]);
            event.setDescription(descriptions[i]);
            event.setType(types[i]);
            event.setCategory(categories[i]);
            event.setCity(cities[i]);
            event.setAddress(addresses[i]);
            event.setStartDateTime(LocalDateTime.now().plusDays((i + 1) * 2));
            event.setEndDateTime(LocalDateTime.now().plusDays((i + 1) * 2).plusHours(3));
            event.setIsFree(isFrees[i]);
            event.setPrice(prices[i]);
            event.setOrganizerName(organizers[i]);
            event.setImageUrl("https://images.loopin.com/event-" + (i + 1) + ".png");
            event.setStatus(EventStatus.PUBLISHED);

            // Assign owner round-robin style from the regular users only
            event.setOwner(owners.get(i % owners.size()));

            events.add(event);
        }

        return eventRepository.saveAll(events);
    }
}
