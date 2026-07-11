package com.loopin.api.events.listmyloopedevents;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.dto.response.LoopedEventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.UserLoopedEvent;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.loopin.LoopedEventCountLoader;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMyLoopedEventsHandler {

    private final EventFinder eventFinder;
    private final UserLoopedEventRepository loopedEventRepository;
    private final EventRepository eventRepository;
    private final LoopedEventCountLoader countLoader;
    private final EventMapper eventMapper;

    public Page<LoopedEventResponse> handle(
        ListMyLoopedEventsQuery query
    ) {
        User currentUser =
            eventFinder.findCurrentUser(query.currentUsername());

        Page<UserLoopedEvent> relationPage =
            loopedEventRepository.findPageByUserId(
                currentUser.getId(),
                query.pageable()
            );

        List<Long> orderedEventIds = relationPage.getContent()
            .stream()
            .map(UserLoopedEvent::getEvent)
            .map(Event::getId)
            .toList();

        if (orderedEventIds.isEmpty()) {
            return new PageImpl<>(
                List.of(),
                query.pageable(),
                relationPage.getTotalElements()
            );
        }

        Map<Long, Event> eventsById =
            eventRepository.findAllByIdWithInterests(orderedEventIds)
                .stream()
                .collect(Collectors.toMap(
                    Event::getId,
                    Function.identity()
                ));

        Map<Long, Long> counts =
            countLoader.load(orderedEventIds);

        List<LoopedEventResponse> responses = orderedEventIds.stream()
            .map(eventsById::get)
            .filter(Objects::nonNull)
            .map(event -> {
                EventResponse eventResponse =
                    eventMapper.toResponse(event);

                long loopedCount =
                    counts.getOrDefault(event.getId(), 0L);

                return new LoopedEventResponse(
                    eventResponse,
                    loopedCount
                );
            })
            .toList();

        return new PageImpl<>(
            responses,
            query.pageable(),
            relationPage.getTotalElements()
        );
    }
}
