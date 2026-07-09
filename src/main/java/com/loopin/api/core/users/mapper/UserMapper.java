package com.loopin.api.core.users.mapper;

import com.loopin.api.core.users.dto.response.UserResponse;
import com.loopin.api.core.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "publicId")
    UserResponse toResponse(User user);
}
