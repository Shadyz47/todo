package com.shop.todo.mapper;

import com.shop.todo.dto.request.UserRequest;
import com.shop.todo.dto.response.UserResponse;
import com.shop.todo.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequest userRequest);

    UserResponse toResponse(User user);
}
