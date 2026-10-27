package com.shop.todo;

import com.shop.todo.entity.Category;
import com.shop.todo.entity.Task;
import com.shop.todo.entity.User;
import com.shop.todo.repository.CategoryRepository;
import com.shop.todo.repository.TaskRepository;
import com.shop.todo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CategoryConflictTests {
    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private TaskRepository tasks;
    private MockMvc mvc;
    private User alice;

    @BeforeEach
    void setUp() {
        clearData();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        alice = createUser("alice");
    }

    @AfterEach
    void clearData() {
        tasks.deleteAllInBatch();
        categories.deleteAllInBatch();
        users.deleteAllInBatch();
    }

    @Test
    void rejectsDuplicateNameForSameOwner() throws Exception {
        createCategory("Study", alice);
        mvc.perform(post("/api/categories").with(user("alice")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Study\"}"))
                .andExpect(status().isConflict());
        assertThat(categories.findAllByUser_Id(alice.getId())).hasSize(1);
    }

    @Test
    void permitsSameNameForDifferentOwners() throws Exception {
        createCategory("Study", createUser("bob"));
        mvc.perform(post("/api/categories").with(user("alice")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Study\"}"))
                .andExpect(status().isCreated());
        assertThat(categories.findAllByUser_Id(alice.getId())).hasSize(1);
        assertThat(categories.count()).isEqualTo(2);
    }

    @Test
    void rejectsRenamingToAnotherCategoryNameWithoutChangingData() throws Exception {
        createCategory("Study", alice);
        Category other = createCategory("Other", alice);
        mvc.perform(put("/api/categories/{id}", other.getId()).with(user("alice")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Study\"}"))
                .andExpect(status().isConflict());
        assertThat(categories.findById(other.getId()).orElseThrow().getName()).isEqualTo("Other");
    }

    @Test
    void permitsKeepingCategoryName() throws Exception {
        Category category = createCategory("Study", alice);
        mvc.perform(put("/api/categories/{id}", category.getId()).with(user("alice")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Study\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsDeletingCategoryWithTasksAndPreservesBoth() throws Exception {
        Category category = createCategory("Study", alice);
        Task task = new Task();
        task.setTitle("Assigned task");
        task.setUser(alice);
        task.setCategory(category);
        tasks.saveAndFlush(task);
        mvc.perform(delete("/api/categories/{id}", category.getId()).with(user("alice")).with(csrf()))
                .andExpect(status().isConflict());
        assertThat(categories.existsById(category.getId())).isTrue();
        assertThat(tasks.existsById(task.getId())).isTrue();
    }

    @Test
    void deletesUnusedCategory() throws Exception {
        Category category = createCategory("Study", alice);
        mvc.perform(delete("/api/categories/{id}", category.getId()).with(user("alice")).with(csrf()))
                .andExpect(status().isOk());
        assertThat(categories.existsById(category.getId())).isFalse();
    }

    @Test
    void rejectsChangesToAnotherUsersCategory() throws Exception {
        Category foreign = createCategory("Study", createUser("bob"));
        mvc.perform(delete("/api/categories/{id}", foreign.getId()).with(user("alice")).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/categories/{id}", foreign.getId()).with(user("alice")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"))
                .andExpect(status().isNotFound());
        assertThat(categories.findById(foreign.getId()).orElseThrow().getName()).isEqualTo("Study");
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-password");
        return users.saveAndFlush(user);
    }

    private Category createCategory(String name, User owner) {
        Category category = new Category();
        category.setName(name);
        category.setUser(owner);
        return categories.saveAndFlush(category);
    }
}
