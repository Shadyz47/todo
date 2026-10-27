package com.shop.todo.repository;

import com.shop.todo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndUser_Id(Long id, Long userId);

    List<Category> findAllByUser_Id(Long id);

    boolean existsByNameAndUser_Id(String name, Long userId);

    boolean existsByNameAndUser_IdAndIdNot(String name, Long userId, Long id);
}
