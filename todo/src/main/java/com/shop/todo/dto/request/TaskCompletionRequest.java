package com.shop.todo.dto.request;

import jakarta.validation.constraints.NotNull;

public record TaskCompletionRequest(
        @NotNull(message = "Completed is required")
        Boolean completed
) {
}
