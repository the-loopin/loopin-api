package com.loopin.api.events.mapper;

import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventInterest;
import com.loopin.api.interests.dto.InterestResponse;
import com.loopin.api.interests.mapper.InterestMapper;
import com.loopin.api.media.mapper.MediaReferenceMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class EventMapper {

    @Autowired
    protected InterestMapper interestMapper;

    @Autowired
    protected MediaReferenceMapper mediaReferenceMapper;

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "imageMedia", ignore = true)
    @Mapping(target = "moderationStatus", ignore = true)
    @Mapping(
        target = "moderationRejectionReason",
        ignore = true
    )
    @Mapping(target = "interests", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    public abstract Event toEntity(
        EventCreateRequest request
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "imageMedia", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "moderationStatus", ignore = true)
    @Mapping(
        target = "moderationRejectionReason",
        ignore = true
    )
    @Mapping(target = "interests", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    public abstract void updateEntity(
        @MappingTarget Event event,
        EventUpdateRequest request
    );

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(
        target = "image",
        expression =
            "java(mediaReferenceMapper.toResponse("
                + "event.getImageMedia()))"
    )
    @Mapping(
        target = "interests",
        expression = "java(mapInterests(event))"
    )
    public abstract EventResponse toResponse(Event event);

    protected List<InterestResponse> mapInterests(
        Event event
    ) {
        if (event.getInterests() == null) {
            return List.of();
        }

        return event.getInterests()
            .stream()
            .map(EventInterest::getInterest)
            .sorted(
                Comparator.comparing(
                    interest ->
                        interest.getName().toLowerCase()
                )
            )
            .map(interestMapper::toResponse)
            .toList();
    }
}
