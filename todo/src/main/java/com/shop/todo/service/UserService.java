package com.shop.todo.service;

import com.shop.todo.dto.request.UserRequest;
import com.shop.todo.dto.response.UserResponse;
import jakarta.validation.Valid;

public interface UserService {
    String registerUser(UserRequest userRequest);

    UserResponse loginUser(@Valid UserRequest userRequest);
}
