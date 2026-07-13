package com.loopin.api.common.cache;

public final class CacheNames {

    /*
     * V2 intentionally avoids reading old PageImpl values
     * already stored under publishedEvents.
     */
    public static final String PUBLISHED_EVENTS = "publishedEventsV2";

    private CacheNames() {
    }
}
