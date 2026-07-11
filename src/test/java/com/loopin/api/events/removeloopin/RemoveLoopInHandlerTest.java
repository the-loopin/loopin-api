package com.loopin.api.events.removeloopin;

import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoveLoopInHandlerTest {

    private EventFinder eventFinder;
    private UserLoopedEventRepository repository;
    private RemoveLoopInHandler handler;

    @BeforeEach
    void setUp() {
        eventFinder = mock(EventFinder.class);
        repository = mock(UserLoopedEventRepository.class);

        handler = new RemoveLoopInHandler(
            eventFinder,
            repository
        );
    }

    @Test
    void handle_ExistingRelation_DeletesByUserAndEventPublicId() {
        String email = "user@example.test";
        UUID eventId = UUID.randomUUID();

        User user = new User(email, "User", null);
        user.setId(42L);

        when(eventFinder.findCurrentUser(email))
            .thenReturn(user);

        handler.handle(
            new RemoveLoopInCommand(eventId, email)
        );

        verify(repository)
            .deleteByUserIdAndEventPublicId(42L, eventId);
    }

    @Test
    void handle_MissingRelation_RemainsIdempotent() {
        String email = "user@example.test";
        UUID eventId = UUID.randomUUID();

        User user = new User(email, "User", null);
        user.setId(42L);

        when(eventFinder.findCurrentUser(email))
            .thenReturn(user);
        when(repository.deleteByUserIdAndEventPublicId(42L, eventId))
            .thenReturn(0);

        handler.handle(
            new RemoveLoopInCommand(eventId, email)
        );

        verify(repository)
            .deleteByUserIdAndEventPublicId(42L, eventId);
    }
}
