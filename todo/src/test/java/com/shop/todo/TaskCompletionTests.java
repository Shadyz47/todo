package com.shop.todo;

import com.shop.todo.entity.Category;
import com.shop.todo.entity.Task;
import com.shop.todo.entity.TaskPriority;
import com.shop.todo.entity.User;
import com.shop.todo.repository.CategoryRepository;
import com.shop.todo.repository.TaskRepository;
import com.shop.todo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class TaskCompletionTests {
    @Autowired private WebApplicationContext context;
    @Autowired private TaskRepository tasks;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private EntityManager entityManager;
    private MockMvc mvc;
    private Long taskId;
    private Long categoryId;
    private final LocalDateTime originalTime = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User owner = new User();
        owner.setUsername("completion-owner");
        owner.setPassword("test-password");
        users.saveAndFlush(owner);
        Category category = new Category();
        category.setName("Study");
        category.setUser(owner);
        categoryId = categories.saveAndFlush(category).getId();
        Task task = new Task();
        task.setTitle("Learn Spring");
        task.setDescription("Keep this description");
        task.setTaskPriority(TaskPriority.HIGH);
        task.setUser(owner);
        task.setCategory(category);
        task.setCreatedAt(originalTime);
        task.setUpdatedAt(originalTime);
        taskId = tasks.saveAndFlush(task).getId();
    }

    @Test
    void completesAndReopensTaskWithoutChangingOtherFields() throws Exception {
        for (boolean completed : new boolean[]{true, false}) {
            mvc.perform(patch("/api/tasks/{id}/completion", taskId)
                            .with(user("completion-owner")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"completed\":" + completed + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(completed));
            tasks.flush();
            entityManager.clear();
            Task saved = tasks.findById(taskId).orElseThrow();
            assertThat(saved.isCompleted()).isEqualTo(completed);
            assertThat(saved.getTitle()).isEqualTo("Learn Spring");
            assertThat(saved.getDescription()).isEqualTo("Keep this description");
            assertThat(saved.getTaskPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(saved.getCategory().getId()).isEqualTo(categoryId);
            assertThat(saved.getCreatedAt()).isEqualTo(originalTime);
            assertThat(saved.getUpdatedAt()).isAfter(originalTime);
            mvc.perform(get("/api/tasks").with(user("completion-owner")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].completed").value(completed));
        }
    }

    @Test
    void rejectsAnotherOwnersTask() throws Exception {
        User other = new User();
        other.setUsername("another-user");
        other.setPassword("test-password");
        users.saveAndFlush(other);
        mvc.perform(patch("/api/tasks/{id}/completion", taskId)
                        .with(user("another-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isNotFound());
        assertThat(tasks.findById(taskId).orElseThrow().isCompleted()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"completed\":null}"})
    void requiresExplicitCompletionValue(String body) throws Exception {
        mvc.perform(patch("/api/tasks/{id}/completion", taskId)
                        .with(user("completion-owner")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(tasks.findById(taskId).orElseThrow().isCompleted()).isFalse();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(patch("/api/tasks/{id}/completion", taskId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresCsrfToken() throws Exception {
        mvc.perform(patch("/api/tasks/{id}/completion", taskId)
                        .with(user("completion-owner"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isForbidden());
    }
}
