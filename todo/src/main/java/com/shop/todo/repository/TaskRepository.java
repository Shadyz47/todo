package com.shop.todo.repository;

import com.shop.todo.entity.Task;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndUser_Id(Long taskId, Long id);

    List<Task> findAllByUser_Id(Long id, Sort by);

    boolean existsByCategory_Id(Long categoryId);

    @Query("""
        SELECT t FROM Task t
        LEFT JOIN t.category category
        WHERE t.user.id = :id
          AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :queryDb, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :queryDb, '%')))
          AND (:start IS NULL OR t.createdAt >= :start)
          AND (:end IS NULL OR t.createdAt < :end)
          AND (:completed IS NULL OR t.completed = :completed)
    """)
    List<Task> findByUser_IdAndQuery(
            @Param("id") Long id,
            @Param("queryDb") String queryDb,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("completed") Boolean completed,
            Sort sort);

    @Query("""
        SELECT t FROM Task t
        WHERE t.user.id = :id
          AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :queryDb, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :queryDb, '%')))
          AND (:start IS NULL OR t.createdAt >= :start)
          AND (:end IS NULL OR t.createdAt < :end)
          AND (:completed IS NULL OR t.completed = :completed)
        ORDER BY CASE t.taskPriority
            WHEN com.shop.todo.entity.TaskPriority.HIGH THEN
                CASE WHEN LOWER(:order) = 'asc' THEN 3 ELSE 1 END
            WHEN com.shop.todo.entity.TaskPriority.MEDIUM THEN 2
            WHEN com.shop.todo.entity.TaskPriority.LOW THEN
                CASE WHEN LOWER(:order) = 'asc' THEN 1 ELSE 3 END
            ELSE 4
        END, t.id ASC
    """)
    List<Task> findByUser_IdAndQueryOrderByPriority(
            @Param("id") Long id,
            @Param("queryDb") String queryDb,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("order") String order,
            @Param("completed") Boolean completed
            );
}
