package com.loopin.api.events.listpublishedevents;

import com.loopin.api.events.dto.response.EventResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CachedEventPage {

    private List<EventResponse> content = new ArrayList<>();

    private long totalElements;

    public CachedEventPage(
        List<EventResponse> content,
        long totalElements
    ) {
        this.content = content == null
            ? new ArrayList<>()
            : new ArrayList<>(content);

        this.totalElements = totalElements;
    }

    public Page<EventResponse> toPage(Pageable pageable) {
        List<EventResponse> safeContent =
            content == null ? List.of() : content;

        return new PageImpl<>(
            safeContent,
            pageable,
            totalElements
        );
    }
}
