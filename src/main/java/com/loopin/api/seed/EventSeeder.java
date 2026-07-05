package com.loopin.api.seed;


import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventRepository;
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
        String[] cities = {"Baku", "Ganja", "Sumqayit", "Shusha"};

        for (int i = 1; i <= 8; i++) {
            Event event = new Event();
            event.setTitle("Tech Loop Conference v" + i);
            event.setDescription("This is the description for the awesome loopin event number " + i);
            event.setType(i % 2 == 0 ? EventType.ACTIVITY : EventType.EVENT);
            event.setCategory(EventCategory.TECH); // Verify TECH exists in EventCategory before running
            event.setCity(cities[i % cities.length]);
            event.setAddress("Street address details for event " + i);
            event.setStartDateTime(LocalDateTime.now().plusDays(i * 2));
            event.setEndDateTime(LocalDateTime.now().plusDays(i * 2).plusHours(4));

            if (i % 3 == 0) {
                event.setIsFree(true);
                event.setPrice(BigDecimal.ZERO);
            } else {
                event.setIsFree(false);
                event.setPrice(new BigDecimal("25.00").multiply(new BigDecimal(i)));
            }

            event.setOrganizerName("Loopin Hub Team");
            event.setImageUrl("https://images.loopin.com/event-" + i + ".png");
            event.setStatus(EventStatus.PUBLISHED);

            // Assign owner round-robin style from the regular users only
            event.setOwner(owners.get(i % owners.size()));

            events.add(event);
        }

        return eventRepository.saveAll(events);
    }
}
