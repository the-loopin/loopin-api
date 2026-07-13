package com.loopin.api.events;

import com.loopin.api.common.cache.CacheNames;
import com.loopin.api.events.cancel.CancelEventHandler;
import com.loopin.api.events.create.CreateEventHandler;
import com.loopin.api.events.delete.DeleteEventHandler;
import com.loopin.api.events.job.EventCompletionProcessor;
import com.loopin.api.events.update.UpdateEventHandler;
import com.loopin.api.moderation.service.AdminServiceImpl;
import com.loopin.api.moderation.service.ModerationReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PublicEventCacheInvalidationTest {

    @Test
    void publicVisibilityWritesEvictPublishedListsAndDetails() throws Exception {
        assertEvicts(CreateEventHandler.class, "handle", true, false);
        assertEvicts(UpdateEventHandler.class, "handle", true, true);
        assertEvicts(DeleteEventHandler.class, "handle", true, true);
        assertEvicts(CancelEventHandler.class, "handle", true, true);
        assertEvicts(ModerationReviewServiceImpl.class, "approveEvent", true, true);
        assertEvicts(ModerationReviewServiceImpl.class, "rejectEvent", true, true);
        assertEvicts(AdminServiceImpl.class, "deleteEvent", true, true);
        assertEvicts(EventCompletionProcessor.class, "completeEvent", true, true);
    }

    private void assertEvicts(Class<?> type, String methodName, boolean published, boolean detail) {
        Method method = Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Caching caching = method.getAnnotation(Caching.class);
        CacheEvict directEviction = method.getAnnotation(CacheEvict.class);
        CacheEvict[] evictions = caching == null
                ? new CacheEvict[]{directEviction}
                : caching.evict();

        assertThat(Arrays.stream(evictions).anyMatch(eviction ->
                eviction.value()[0].equals(CacheNames.PUBLISHED_EVENTS) && eviction.allEntries())).isEqualTo(published);
        assertThat(Arrays.stream(evictions).anyMatch(eviction ->
                eviction.value()[0].equals("eventById"))).isEqualTo(detail);
    }
}
