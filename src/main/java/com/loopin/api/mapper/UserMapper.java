package com.loopin.api.mapper;

import com.loopin.api.dto.user.response.UserResponse;
import com.loopin.api.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "publicId")
    UserResponse toResponse(User user);
}
