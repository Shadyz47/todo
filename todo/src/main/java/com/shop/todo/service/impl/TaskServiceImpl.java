package com.shop.todo.service.impl;

import com.shop.todo.dto.request.TaskRequest;
import com.shop.todo.dto.response.TaskResponse;
import com.shop.todo.entity.Category;
import com.shop.todo.entity.Task;
import com.shop.todo.entity.TaskPriority;
import com.shop.todo.entity.User;
import com.shop.todo.mapper.TaskMapper;
import com.shop.todo.repository.CategoryRepository;
import com.shop.todo.repository.TaskRepository;
import com.shop.todo.repository.UserRepository;
import com.shop.todo.service.TaskService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepo;
    private final TaskMapper taskMapper;
    private final CategoryRepository categoryRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public String createTask(TaskRequest taskRequest, String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        Task task = taskMapper.toTask(taskRequest);
        task.setUser(user);

        if (taskRequest.categoryId() != null) {
            task.setCategory(categoryRepo.findByIdAndUser_Id(taskRequest.categoryId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Category not found")));
        }

        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        taskRepo.save(task);

        return "Task created successfully";
    }

    @Override
    @Transactional
    public String updateTask(Long taskId, TaskRequest request, String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        Task task = taskRepo.findByIdAndUser_Id(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepo.findByIdAndUser_Id(
                            request.categoryId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Category not found"));
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setTaskPriority(
                request.taskPriority() != null
                        ? request.taskPriority()
                        : TaskPriority.MEDIUM
        );
        task.setCategory(category);
        task.setUpdatedAt(LocalDateTime.now());

        taskRepo.save(task);

        return "Task updated successfully";
    }

    @Override
    @Transactional
    public TaskResponse updateTaskCompletion(Long taskId, boolean completed, String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        Task task = taskRepo.findByIdAndUser_Id(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setCompleted(completed);
        task.setUpdatedAt(LocalDateTime.now());
        return taskMapper.toTaskResponse(taskRepo.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> searchTasks(String query, LocalDate fromDate, LocalDate toDate, String sortBy, Boolean completed, String order, String username) {

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must not be after toDate");
        }

        String queryDb = query == null ? "" : query.trim();

        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end  = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        String requestedSort = sortBy == null ? "priority" : sortBy;
        Sort.Direction direction = "category".equals(requestedSort)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (order != null) {
            try {
                direction = Sort.Direction.fromString(order);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order must be asc or desc");
            }
        }

        List<Task> tasks;
        if ("priority".equals(requestedSort)) {
            tasks = taskRepo.findByUser_IdAndQueryOrderByPriority(
                    user.getId(), queryDb, start, end, direction.name(), completed);
        } else {
            Sort sort = switch (requestedSort) {
                case "createdAt" -> Sort.by(new Sort.Order(direction, "createdAt"), Sort.Order.asc("id"));
                case "updatedAt" -> Sort.by(new Sort.Order(direction, "updatedAt"), Sort.Order.asc("id"));
                case "category" -> Sort.by(new Sort.Order(direction, "category.name"), Sort.Order.asc("id"));
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "sortBy must be priority, createdAt, updatedAt or category");
            };
            tasks = taskRepo.findByUser_IdAndQuery(
                    user.getId(), queryDb, start, end, completed, sort);
        }

        return taskMapper.toTaskResponseList(tasks);
    }

    @Override
    public String deleteTask(Long taskId, String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        Task task = taskRepo.findByIdAndUser_Id(taskId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));

        taskRepo.delete(task);

        return "Task deleted successfully";
    }
}
