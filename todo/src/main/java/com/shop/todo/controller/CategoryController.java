package com.shop.todo.controller;

import com.shop.todo.dto.request.CategoryRequest;
import com.shop.todo.dto.response.CategoryResponse;
import com.shop.todo.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@AllArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(Principal principal) {
        String username = principal == null ? null : principal.getName();
        return ResponseEntity.ok(categoryService.getAllCategories(username));
    }

    @PostMapping
    public ResponseEntity<String> createCategory(@Valid @RequestBody CategoryRequest request, Principal principal) {
        String username = principal == null ? null : principal.getName();
        return new ResponseEntity<>(categoryService.createCategory(request, username), HttpStatus.CREATED);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<String> updateCategory(@PathVariable Long categoryId,
                                                 @Valid @RequestBody CategoryRequest request,
                                                 Principal principal) {
        String username = principal == null ? null : principal.getName();
        return new ResponseEntity<>(categoryService.updateCategory(categoryId, request, username), HttpStatus.OK);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId, Principal principal) {
        String username = principal == null ? null : principal.getName();
        return new ResponseEntity<>(categoryService.deleteCategory(categoryId, username), HttpStatus.OK);
    }
}
