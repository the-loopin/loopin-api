package com.loopin.api.events.removeloopin;

import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RemoveLoopInHandler {

    private final EventFinder eventFinder;
    private final UserLoopedEventRepository loopedEventRepository;

    @Transactional
    public void handle(RemoveLoopInCommand command) {
        User currentUser =
            eventFinder.findCurrentUser(command.currentUsername());

        loopedEventRepository.deleteByUserIdAndEventPublicId(
            currentUser.getId(),
            command.eventId()
        );
    }
}
