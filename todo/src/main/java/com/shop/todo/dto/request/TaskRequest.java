package com.shop.todo.dto.request;

import com.shop.todo.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;

public record TaskRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        TaskPriority taskPriority,

        Long categoryId
) {
}
