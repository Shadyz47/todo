package com.shop.todo.dto.response;

import com.shop.todo.entity.TaskPriority;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskPriority taskPriority,
        boolean completed,
        LocalDateTime createdAt,
        String categoryName,
        Long categoryId
) {
}
