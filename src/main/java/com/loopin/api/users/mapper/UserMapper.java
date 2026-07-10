package com.loopin.api.users.mapper;

import com.loopin.api.users.dto.response.UserResponse;
import com.loopin.api.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "publicId")
    UserResponse toResponse(User user);
}
