package com.shop.todo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:session-http-test;DB_CLOSE_DELAY=-1")
class SessionCookieHttpTests {
    @LocalServerPort
    private int port;

    @Test
    void sessionCookieAuthenticatesRealHttpRequestsAfterLogin() throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        try (HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build()) {
            HttpResponse<String> csrf = client.send(get("/api/users/csrf"), HttpResponse.BodyHandlers.ofString());
            assertThat(csrf.statusCode()).isEqualTo(200);
            assertThat(cookies.getCookieStore().getCookies()).anyMatch(cookie -> cookie.getName().equals("JSESSIONID"));
            String header = JsonPath.read(csrf.body(), "$.headerName");
            String token = JsonPath.read(csrf.body(), "$.token");
            String credentials = "{\"username\":\"http-user\",\"password\":\"test-password\"}";

            HttpResponse<String> registration = client.send(post("/api/users/register", header, token, credentials),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(registration.statusCode()).isEqualTo(201);

            HttpResponse<String> login = client.send(post("/api/users/login", header, token, credentials),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(login.statusCode()).isEqualTo(200);

            HttpResponse<String> tasks = client.send(get("/api/tasks"), HttpResponse.BodyHandlers.ofString());
            assertThat(tasks.statusCode()).isEqualTo(200);

            HttpResponse<String> refreshed = client.send(get("/api/users/csrf"), HttpResponse.BodyHandlers.ofString());
            String refreshedToken = JsonPath.read(refreshed.body(), "$.token");
            HttpResponse<String> created = client.send(post("/api/tasks", header, refreshedToken,
                            "{\"title\":\"HTTP task\",\"description\":\"Created using a session cookie\"}"),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(created.statusCode()).isEqualTo(201);

            HttpResponse<String> listed = client.send(get("/api/tasks"), HttpResponse.BodyHandlers.ofString());
            assertThat(listed.statusCode()).isEqualTo(200);
            assertThat(JsonPath.<String>read(listed.body(), "$[0].title")).isEqualTo("HTTP task");
        }
        try (HttpClient anonymous = HttpClient.newHttpClient()) {
            HttpResponse<String> result = anonymous.send(get("/api/tasks"), HttpResponse.BodyHandlers.ofString());
            assertThat(result.statusCode()).isEqualTo(401);
        }
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    }

    private HttpRequest post(String path, String csrfHeader, String csrfToken, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header(csrfHeader, csrfToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }
}
