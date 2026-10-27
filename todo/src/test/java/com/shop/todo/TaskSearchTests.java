package com.shop.todo;

import com.jayway.jsonpath.JsonPath;
import com.shop.todo.entity.Category;
import com.shop.todo.entity.Task;
import com.shop.todo.entity.TaskPriority;
import com.shop.todo.entity.User;
import com.shop.todo.repository.CategoryRepository;
import com.shop.todo.repository.TaskRepository;
import com.shop.todo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class TaskSearchTests {
    private static final LocalDateTime DAY = LocalDateTime.of(2026, 10, 20, 0, 0);

    @Autowired private WebApplicationContext context;
    @Autowired private TaskRepository tasks;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;

    private MockMvc mvc;
    private User owner;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = createUser("search-owner");
    }

    @ParameterizedTest
    @CsvSource({
            "priority,Read docs,Learn SPRING",
            "createdAt,Read docs,Learn SPRING",
            "updatedAt,Learn SPRING,Read docs",
            "category,Read docs,Learn SPRING"
    })
    void combinesEveryFilterWithEachSort(String sortBy, String first, String second) throws Exception {
        Category alpha = createCategory("Alpha");
        Category zulu = createCategory("Zulu");
        createTask(owner, "Learn SPRING", null, TaskPriority.LOW, true,
                DAY, DAY.plusDays(2), zulu);
        createTask(owner, "Read docs", "Spring DATA JPA", TaskPriority.HIGH, true,
                DAY.plusDays(1).minusNanos(1_000), DAY.plusDays(1), alpha);

        createTask(owner, "Unrelated", "Other topic", TaskPriority.HIGH, true, DAY, DAY, alpha);
        createTask(owner, "Spring incomplete", null, TaskPriority.HIGH, false, DAY, DAY, alpha);
        createTask(owner, "Spring before", null, TaskPriority.HIGH, true,
                DAY.minusNanos(1_000), DAY, alpha);
        createTask(owner, "Spring after", null, TaskPriority.HIGH, true,
                DAY.plusDays(1), DAY, alpha);
        createTask(createUser("other-owner"), "Other user's task", "Spring", TaskPriority.HIGH,
                true, DAY, DAY, null);

        mvc.perform(get("/api/tasks").with(user(owner.getUsername()))
                        .param("query", "  sPrInG  ")
                        .param("fromDate", "2026-10-20")
                        .param("toDate", "2026-10-20")
                        .param("completed", "true")
                        .param("sortBy", sortBy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains(first, second)));
    }

    @Test
    void defaultsToAllCompletionStatesSortedByPriority() throws Exception {
        createTask("Low", TaskPriority.LOW, false);
        createTask("Medium", TaskPriority.MEDIUM, true);
        createTask("High first", TaskPriority.HIGH, true);
        createTask("High second", TaskPriority.HIGH, false);
        createTask(createUser("other-owner"), "Hidden", null, TaskPriority.HIGH,
                false, DAY, DAY, null);

        mvc.perform(get("/api/tasks").with(user(owner.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title",
                        contains("High first", "High second", "Medium", "Low")));
    }

    @ParameterizedTest
    @CsvSource({
            "asc,Low,Medium,High first,High second",
            "desc,High first,High second,Medium,Low",
            "ASC,Low,Medium,High first,High second",
            "DESC,High first,High second,Medium,Low"
    })
    void sortsPriorityInRequestedOrder(String order, String first, String second,
                                       String third, String fourth) throws Exception {
        createTask("Medium", TaskPriority.MEDIUM, false);
        createTask("High first", TaskPriority.HIGH, false);
        createTask("Low", TaskPriority.LOW, false);
        createTask("High second", TaskPriority.HIGH, false);

        mvc.perform(get("/api/tasks").with(user(owner.getUsername()))
                        .param("sortBy", "priority").param("order", order))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains(first, second, third, fourth)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void filtersExplicitCompletionState(boolean completed) throws Exception {
        createTask("Done", TaskPriority.MEDIUM, true);
        createTask("Pending", TaskPriority.MEDIUM, false);

        mvc.perform(get("/api/tasks").with(user(owner.getUsername()))
                        .param("completed", Boolean.toString(completed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains(completed ? "Done" : "Pending")));
    }

    @ParameterizedTest
    @CsvSource({
            "fromDate,2026-10-20,After,Inside",
            "toDate,2026-10-20,Inside,Before"
    })
    void supportsOneSidedDateRanges(String parameter, String date, String first, String second)
            throws Exception {
        createTask(owner, "Before", null, TaskPriority.MEDIUM, false,
                DAY.minusNanos(1_000), DAY, null);
        createTask("Inside", TaskPriority.MEDIUM, false);
        createTask(owner, "After", null, TaskPriority.MEDIUM, false,
                DAY.plusDays(1), DAY, null);

        mvc.perform(get("/api/tasks").with(user(owner.getUsername()))
                        .param(parameter, date).param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains(first, second)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"priority", "createdAt", "updatedAt", "category"})
    void usesIdAsTieBreaker(String sortBy) throws Exception {
        Category category = createCategory("Study");
        createTask(owner, "First", null, TaskPriority.MEDIUM, false, DAY, DAY, category);
        createTask(owner, "Second", null, TaskPriority.MEDIUM, false, DAY, DAY, category);

        mvc.perform(get("/api/tasks").with(user(owner.getUsername())).param("sortBy", sortBy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("First", "Second")));
    }

    @Test
    void categorySortKeepsTasksWithoutCategory() throws Exception {
        createTask(owner, "Zulu task", null, TaskPriority.MEDIUM, false,
                DAY, DAY, createCategory("Zulu"));
        createTask("Uncategorized task", TaskPriority.MEDIUM, false);
        createTask(owner, "Alpha task", null, TaskPriority.MEDIUM, false,
                DAY, DAY, createCategory("Alpha"));

        String response = mvc.perform(get("/api/tasks").with(user(owner.getUsername()))
                        .param("query", "task").param("sortBy", "category"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> titles = JsonPath.read(response, "$[*].title");
        assertThat(titles).containsExactlyInAnyOrder("Zulu task", "Uncategorized task", "Alpha task");
        assertThat(titles.indexOf("Alpha task")).isLessThan(titles.indexOf("Zulu task"));
    }

    @Test
    void returnsEmptyArrayWhenNothingMatches() throws Exception {
        createTask("Learn Spring", TaskPriority.HIGH, false);

        mvc.perform(get("/api/tasks").with(user(owner.getUsername())).param("query", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsReversedDateRange() throws Exception {
        mvc.perform(get("/api/tasks").with(user(owner.getUsername()))
                        .param("fromDate", "2026-10-21").param("toDate", "2026-10-20"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "sortBy,unknown",
            "order,invalid",
            "fromDate,not-a-date",
            "toDate,2026-99-99",
            "completed,invalid"
    })
    void rejectsInvalidParameters(String parameter, String value) throws Exception {
        mvc.perform(get("/api/tasks").with(user(owner.getUsername())).param(parameter, value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/tasks").param("query", "spring"))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-password");
        return users.saveAndFlush(user);
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setUser(owner);
        return categories.saveAndFlush(category);
    }

    private void createTask(String title, TaskPriority priority, boolean completed) {
        createTask(owner, title, null, priority, completed, DAY, DAY, null);
    }

    private void createTask(User taskOwner, String title, String description, TaskPriority priority,
                            boolean completed, LocalDateTime createdAt, LocalDateTime updatedAt,
                            Category category) {
        Task task = new Task();
        task.setUser(taskOwner);
        task.setTitle(title);
        task.setDescription(description);
        task.setTaskPriority(priority);
        task.setCompleted(completed);
        task.setCreatedAt(createdAt);
        task.setUpdatedAt(updatedAt);
        task.setCategory(category);
        tasks.saveAndFlush(task);
    }
}
