package com.shop.todo.service.impl;

import com.shop.todo.dto.request.CategoryRequest;
import com.shop.todo.dto.response.CategoryResponse;
import com.shop.todo.entity.Category;
import com.shop.todo.entity.User;
import com.shop.todo.mapper.CategoryMapper;
import com.shop.todo.repository.CategoryRepository;
import com.shop.todo.repository.TaskRepository;
import com.shop.todo.repository.UserRepository;
import com.shop.todo.service.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepo;
    private final UserRepository userRepo;
    private final CategoryMapper categoryMapper;
    private final TaskRepository taskRepo;

    @Override
    @Transactional
    public String createCategory(CategoryRequest request, String username) {

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (categoryRepo.existsByNameAndUser_Id(request.name(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        Category category = categoryMapper.toCategory(request);
        category.setUser(user);

        categoryRepo.saveAndFlush(category);
        return "Category created successfully";
    }

    @Override
    @Transactional
    public String updateCategory(Long categoryId, CategoryRequest request, String username) {

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        Category category = categoryRepo.findByIdAndUser_Id(categoryId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));

        if (categoryRepo.existsByNameAndUser_IdAndIdNot(request.name(), user.getId(), categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        category.setName(request.name());
        categoryRepo.saveAndFlush(category);

        return "Updated category: " + category.getName() + "Successfully updated";
    }

    @Override
    @Transactional
    public String deleteCategory(Long categoryId, String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        Category category = categoryRepo.findByIdAndUser_Id(categoryId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));

        if (taskRepo.existsByCategory_Id(categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category is used by one or more tasks");
        }

        categoryRepo.delete(category);
        categoryRepo.flush();
        return "Deleted category: " + category.getName();
    }

    @Override
    public List<CategoryResponse> getAllCategories(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        List<Category> categories = categoryRepo.findAllByUser_Id(user.getId());
        return categoryMapper.toCategoryResponseList(categories);
    }


}
