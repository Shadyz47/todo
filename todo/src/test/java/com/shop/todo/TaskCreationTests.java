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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class TaskCreationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        user = createUser("alice");
    }

    @Test
    void createsTaskWithOwnedCategoryAndRequestedPriority() throws Exception {
        Category category = createCategory(user);

        mockMvc.perform(post("/api/tasks")
                        .with(user(user.getUsername())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Learn Spring",
                                  "description": "Create a task",
                                  "taskPriority": "HIGH",
                                  "categoryId": %d
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(content().string("Task created successfully"));

        Task saved = readOnlyTask();
        assertThat(saved.getTitle()).isEqualTo("Learn Spring");
        assertThat(saved.getDescription()).isEqualTo("Create a task");
        assertThat(saved.getTaskPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void createsTaskWithoutCategoryUsingDefaultPriority() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(user(user.getUsername())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Learn Spring", "description": "Create a task"}
                                """))
                .andExpect(status().isCreated());

        Task saved = readOnlyTask();
        assertThat(saved.getCategory()).isNull();
        assertThat(saved.getTaskPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void rejectsMissingCategoryWithoutSavingTask() throws Exception {
        assertCategoryRejected(-1L);
    }

    @Test
    void rejectsCategoryOwnedByAnotherUserWithoutSavingTask() throws Exception {
        Category category = createCategory(createUser("bob"));

        assertCategoryRejected(category.getId());
    }

    @Test
    void rejectsUnauthenticatedRequestWithoutSavingTask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Learn Spring", "description": "Create a task"}
                                """))
                .andExpect(status().isUnauthorized());

        assertThat(taskRepo.count()).isZero();
    }

    @Test
    void rejectsPrincipalWithNoStoredUserWithoutSavingTask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(user("unknown-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Learn Spring", "description": "Create a task"}
                                """))
                .andExpect(status().isUnauthorized());

        assertThat(taskRepo.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\": \"   \", \"description\": \"Create a task\"}",
            "{\"title\": \"Learn Spring\", \"description\": \"   \"}"
    })
    void rejectsBlankRequiredFieldsWithoutSavingTask(String requestBody) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(user(user.getUsername())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(taskRepo.count()).isZero();
    }

    private void assertCategoryRejected(Long categoryId) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(user(user.getUsername())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Learn Spring", "description": "Create a task", "categoryId": %d}
                                """.formatted(categoryId)))
                .andExpect(status().isNotFound());

        assertThat(taskRepo.count()).isZero();
    }

    private User createUser(String username) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword("test-only-password");
        return userRepo.saveAndFlush(newUser);
    }

    private Category createCategory(User owner) {
        Category category = new Category();
        category.setName("Study");
        category.setUser(owner);
        return categoryRepo.saveAndFlush(category);
    }

    private Task readOnlyTask() {
        taskRepo.flush();
        entityManager.clear();
        assertThat(taskRepo.findAll()).hasSize(1);
        return taskRepo.findAll().getFirst();
    }
}
