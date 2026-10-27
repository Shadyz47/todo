package com.shop.todo.service;

import com.shop.todo.dto.request.TaskRequest;
import com.shop.todo.dto.response.TaskResponse;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

public interface TaskService {

    String createTask(TaskRequest taskRequest, String username);

    String updateTask(Long taskId, @Valid TaskRequest taskRequest, String username);

    String deleteTask(Long taskId, String username);

    TaskResponse updateTaskCompletion(Long taskId, boolean completed, String username);

    List<TaskResponse> searchTasks(String query, LocalDate fromDate, LocalDate toDate, String sortBy, Boolean completed, String order, String username);
}
