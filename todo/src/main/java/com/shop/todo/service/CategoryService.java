package com.shop.todo.service;

import com.shop.todo.dto.request.CategoryRequest;
import com.shop.todo.dto.response.CategoryResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface CategoryService {

    String createCategory(@Valid CategoryRequest request, String username);

    String updateCategory(Long categoryId, @Valid CategoryRequest request, String username);

    String deleteCategory(Long categoryId, String username);

    List<CategoryResponse> getAllCategories(String username);
}
