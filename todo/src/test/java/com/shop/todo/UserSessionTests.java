package com.shop.todo;

import com.jayway.jsonpath.JsonPath;
import com.shop.todo.entity.User;
import com.shop.todo.repository.CategoryRepository;
import com.shop.todo.repository.TaskRepository;
import com.shop.todo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserSessionTests {
    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    @Autowired private TaskRepository tasks;
    @Autowired private CategoryRepository categories;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        clearData();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void clearData() {
        tasks.deleteAllInBatch();
        categories.deleteAllInBatch();
        users.deleteAllInBatch();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8082", "http://127.0.0.1:8082"})
    void allowsFrontendToReadCsrfWithSessionCookies(String origin) throws Exception {
        mvc.perform(get("/api/users/csrf").header("Origin", origin))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "http://localhost:8082,/api/users/login",
            "http://127.0.0.1:8082,/api/users/login",
            "http://localhost:8082,/api/users/register",
            "http://127.0.0.1:8082,/api/users/register"
    })
    void allowsAnonymousFrontendPreflight(String origin, String path) throws Exception {
        mvc.perform(options(path)
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsString("x-csrf-token")));
    }

    @Test
    void rejectsUntrustedOrigin() throws Exception {
        mvc.perform(get("/api/users/csrf").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void registersNewUsernameAndRejectsDuplicateWithoutChangingPassword() throws Exception {
        CsrfSession csrf = csrf(null);
        mvc.perform(post("/api/users/register").session(csrf.session())
                        .header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "test-password")))
                .andExpect(status().isCreated());
        User registered = users.findByUsername("alice").orElseThrow();
        String originalPassword = registered.getPassword();
        mvc.perform(post("/api/users/register").session(csrf.session())
                        .header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "different-password")))
                .andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByUsername("alice").orElseThrow().getPassword()).isEqualTo(originalPassword);
    }

    @Test
    void loginRotatesSessionAndAuthenticatesLaterRequests() throws Exception {
        seedUser();
        CsrfSession beforeLogin = csrf(null);
        String oldSessionId = beforeLogin.session().getId();
        MockHttpSession session = login(beforeLogin);
        assertThat(session.getId()).isNotEqualTo(oldSessionId);

        mvc.perform(get("/api/tasks").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/categories").session(session)).andExpect(status().isOk());

        mvc.perform(post("/api/tasks").session(session)
                        .header(beforeLogin.header(), beforeLogin.token())
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody()))
                .andExpect(status().isForbidden());

        CsrfSession authenticatedCsrf = csrf(session);
        mvc.perform(post("/api/tasks").session(session)
                        .header(authenticatedCsrf.header(), authenticatedCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody()))
                .andExpect(status().isCreated());
        assertThat(tasks.findAllByUser_Id(users.findByUsername("alice").orElseThrow().getId(),
                org.springframework.data.domain.Sort.unsorted())).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({"alice,wrong-password", "unknown,test-password"})
    void rejectsInvalidCredentialsWithoutAuthenticatingSession(String username, String password) throws Exception {
        seedUser();
        CsrfSession csrf = csrf(null);
        mvc.perform(post("/api/users/login").session(csrf.session())
                        .header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(credentials(username, password)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tasks").session(csrf.session())).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginWithoutCsrfToken() throws Exception {
        seedUser();
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "test-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        seedUser();
        MockHttpSession session = login(csrf(null));
        CsrfSession csrf = csrf(session);
        mvc.perform(post("/api/users/logout").session(session).header(csrf.header(), csrf.token()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(CsrfSession csrf) throws Exception {
        MvcResult result = mvc.perform(post("/api/users/login").session(csrf.session())
                        .header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(credentials("alice", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private CsrfSession csrf(MockHttpSession session) throws Exception {
        var request = get("/api/users/csrf");
        if (session != null) {
            request.session(session);
        }
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        return new CsrfSession((MockHttpSession) result.getRequest().getSession(false),
                JsonPath.read(body, "$.headerName"), JsonPath.read(body, "$.token"));
    }

    private void seedUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("test-password");
        users.saveAndFlush(user);
    }

    private String credentials(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    private String taskBody() {
        return "{\"title\":\"Session task\",\"description\":\"Created after login\"}";
    }

    private record CsrfSession(MockHttpSession session, String header, String token) {}
}
