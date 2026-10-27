package com.shop.todo.controller;

import com.shop.todo.dto.request.TaskRequest;
import com.shop.todo.dto.request.TaskCompletionRequest;
import com.shop.todo.dto.response.TaskResponse;
import com.shop.todo.service.TaskService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@AllArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> searchTasks(@RequestParam(defaultValue = "") String query,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                          @RequestParam(defaultValue = "priority") String sortBy,
                                                          @RequestParam(required = false) Boolean completed,
                                                          @RequestParam(required = false) String order,
                                                          Principal principal) {
        String username = principal == null ? null : principal.getName();
        return ResponseEntity.ok(taskService.searchTasks(query, fromDate, toDate, sortBy, completed, order, username));
    }

    @PostMapping
    public ResponseEntity<String> createTask(@Valid @RequestBody TaskRequest taskRequest,
                                             Principal principal){
        String username = principal == null ? null : principal.getName();
        return new ResponseEntity<>(taskService.createTask(taskRequest, username), HttpStatus.CREATED);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<String> updateTask(@PathVariable Long taskId,
                                             @Valid @RequestBody TaskRequest taskRequest,
                                             Principal principal){
        String username = principal == null ? null : principal.getName();
        return new ResponseEntity<>(taskService.updateTask(taskId, taskRequest, username), HttpStatus.OK);
    }

    @PatchMapping("/{taskId}/completion")
    public ResponseEntity<TaskResponse> updateTaskCompletion(@PathVariable Long taskId,
                                                              @Valid @RequestBody TaskCompletionRequest request,
                                                              Principal principal) {
        String username = principal == null ? null : principal.getName();
        return ResponseEntity.ok(taskService.updateTaskCompletion(taskId, request.completed(), username));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId,
                                             Principal principal) {
        String username = principal == null ? null : principal.getName();
        return new ResponseEntity<>(taskService.deleteTask(taskId, username), HttpStatus.OK);
    }
}
