package com.shop.todo.service.impl;

import com.shop.todo.dto.request.UserRequest;
import com.shop.todo.dto.response.UserResponse;
import com.shop.todo.entity.User;
import com.shop.todo.mapper.UserMapper;
import com.shop.todo.repository.UserRepository;
import com.shop.todo.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public String registerUser(UserRequest userRequest) {
        if (userRepo.existsByUsername(userRequest.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = userMapper.toEntity(userRequest);
        userRepo.saveAndFlush(user);

        return "Registered user: " + user.getUsername() + " with ID: " + user.getId();
    }

    @Override
    public UserResponse loginUser(UserRequest userRequest) {

        User user = userRepo.findByUsernameAndPassword(userRequest.username(), userRequest.password()).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password")
        );

        UserResponse userRp = userMapper.toResponse(user);

        return userRp;
    }
}
