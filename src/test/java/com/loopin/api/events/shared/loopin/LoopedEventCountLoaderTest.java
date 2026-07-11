package com.loopin.api.events.shared.loopin;

import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.repository.projection.LoopedEventCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoopedEventCountLoaderTest {

    private UserLoopedEventRepository repository;
    private LoopedEventCountLoader loader;

    @BeforeEach
    void setUp() {
        repository = mock(UserLoopedEventRepository.class);
        loader = new LoopedEventCountLoader(repository);
    }

    @Test
    void load_EmptyIds_ReturnsEmptyMapWithoutQuery() {
        Map<Long, Long> result = loader.load(List.of());

        assertEquals(Map.of(), result);
        verify(repository, never()).countByEventIds(List.of());
    }

    @Test
    void load_EventIds_ReturnsCountMap() {
        LoopedEventCountProjection first =
            projection(10L, 5L);
        LoopedEventCountProjection second =
            projection(20L, 3L);

        when(repository.countByEventIds(List.of(10L, 20L)))
            .thenReturn(List.of(first, second));

        Map<Long, Long> result =
            loader.load(List.of(10L, 20L));

        assertEquals(5L, result.get(10L));
        assertEquals(3L, result.get(20L));
    }

    private LoopedEventCountProjection projection(
        Long eventId,
        Long count
    ) {
        LoopedEventCountProjection projection =
            mock(LoopedEventCountProjection.class);

        when(projection.getEventId()).thenReturn(eventId);
        when(projection.getLoopedCount()).thenReturn(count);

        return projection;
    }
}
