package com.tyj.campuscircle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CampusCircleApiIntegrationTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void campusHubCoreApiFlow() throws Exception {
        register("alice", "123456", "小艾");
        register("bob", "123456", "小林");

        String aliceToken = login("alice", "123456");
        String bobToken = login("bob", "123456");

        Long categoryId = firstCategoryId();
        Long postId = createPost(aliceToken, categoryId);
        Long commentId = createComment(bobToken, postId);

        likePost(bobToken, postId);

        JsonNode comments = get("/api/posts/" + postId + "/comments", null);
        assertThat(comments.at("/code").asInt()).isZero();
        assertThat(comments.at("/data/total").asLong()).isEqualTo(1);
        assertThat(comments.at("/data/records/0/id").asLong()).isEqualTo(commentId);

        JsonNode likeStatus = get("/api/posts/" + postId + "/like", bobToken);
        assertThat(likeStatus.at("/code").asInt()).isZero();
        assertThat(likeStatus.at("/data/liked").asBoolean()).isTrue();

        JsonNode unreadCount = get("/api/notices/unread-count", aliceToken);
        assertThat(unreadCount.at("/code").asInt()).isZero();
        assertThat(unreadCount.at("/data/count").asLong()).isEqualTo(2);

        JsonNode notices = get("/api/notices", aliceToken);
        assertThat(notices.at("/code").asInt()).isZero();
        assertThat(notices.at("/data/total").asLong()).isEqualTo(2);
    }

    @Test
    void campusHubCoreApiBoundaryFlow() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String authorUsername = "author_" + suffix;
        String readerUsername = "reader_" + suffix;

        Long categoryId = firstCategoryId();

        JsonNode unauthorizedCreatePost = post("/api/posts", null, Map.of(
                "categoryId", categoryId,
                "title", "No token post",
                "content", "This request should be rejected."
        ));
        assertCode(unauthorizedCreatePost, 40100);

        register(authorUsername, "123456", "Author");
        JsonNode duplicateRegister = post("/api/auth/register", null, Map.of(
                "username", authorUsername,
                "password", "123456",
                "nickname", "Author Again"
        ));
        assertCode(duplicateRegister, 40901);

        String authorToken = login(authorUsername, "123456");
        register(readerUsername, "123456", "Reader");
        String readerToken = login(readerUsername, "123456");

        Long postId = createPost(authorToken, categoryId);

        JsonNode invalidPage = get("/api/posts?page=0", null);
        assertCode(invalidPage, 40000);

        JsonNode forbiddenAdmin = put("/api/admin/posts/" + postId + "/hide", readerToken, null);
        assertCode(forbiddenAdmin, 40300);

        JsonNode firstLike = post("/api/posts/" + postId + "/like", readerToken, null);
        assertCode(firstLike, 0);
        assertThat(firstLike.at("/data/likeCount").asInt()).isEqualTo(1);

        JsonNode duplicateLike = post("/api/posts/" + postId + "/like", readerToken, null);
        assertCode(duplicateLike, 0);
        assertThat(duplicateLike.at("/data/likeCount").asInt()).isEqualTo(1);

        JsonNode firstUnlike = delete("/api/posts/" + postId + "/like", readerToken);
        assertCode(firstUnlike, 0);
        assertThat(firstUnlike.at("/data/likeCount").asInt()).isEqualTo(0);

        JsonNode duplicateUnlike = delete("/api/posts/" + postId + "/like", readerToken);
        assertCode(duplicateUnlike, 0);
        assertThat(duplicateUnlike.at("/data/likeCount").asInt()).isEqualTo(0);
    }

    private void register(String username, String password, String nickname) throws Exception {
        JsonNode response = post("/api/auth/register", null, Map.of(
                "username", username,
                "password", password,
                "nickname", nickname
        ));
        assertThat(response.at("/code").asInt())
                .describedAs(response.toPrettyString())
                .isZero();
    }

    private String login(String username, String password) throws Exception {
        JsonNode response = post("/api/auth/login", null, Map.of(
                "username", username,
                "password", password
        ));

        assertThat(response.at("/code").asInt())
                .describedAs(response.toPrettyString())
                .isZero();
        return response.at("/data/token").asText();
    }

    private Long firstCategoryId() throws Exception {
        JsonNode response = get("/api/categories", null);
        assertThat(response.at("/code").asInt()).isZero();
        assertThat(response.at("/data").size()).isEqualTo(6);

        return response.at("/data/0/id").asLong();
    }

    private Long createPost(String token, Long categoryId) throws Exception {
        JsonNode response = post("/api/posts", token, Map.of(
                "categoryId", categoryId,
                "title", "高数复习资料怎么整理？",
                "content", "想问问大家期末复习有什么方法。"
        ));

        assertThat(response.at("/code").asInt())
                .describedAs(response.toPrettyString())
                .isZero();
        Long postId = response.at("/data/postId").asLong();
        assertThat(postId).isPositive();
        return postId;
    }

    private Long createComment(String token, Long postId) throws Exception {
        JsonNode response = post("/api/posts/" + postId + "/comments", token, Map.of("content", "我一般先整理错题，再刷历年卷。"));
        assertThat(response.at("/code").asInt()).isZero();
        Long commentId = response.at("/data/commentId").asLong();
        assertThat(commentId).isPositive();
        return commentId;
    }

    private void likePost(String token, Long postId) throws Exception {
        JsonNode response = post("/api/posts/" + postId + "/like", token, null);
        assertThat(response.at("/code").asInt()).isZero();
        assertThat(response.at("/data/liked").asBoolean()).isTrue();
        assertThat(response.at("/data/likeCount").asInt()).isEqualTo(1);
    }

    private JsonNode get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .GET()
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);
        addAuthorization(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private JsonNode put(String path, String token, Object body) throws Exception {
        String requestBody = body == null ? "" : objectMapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);
        addAuthorization(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private JsonNode delete(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .DELETE()
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);
        addAuthorization(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private JsonNode post(String path, String token, Object body) throws Exception {
        String requestBody = body == null ? "" : objectMapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);
        addAuthorization(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private void assertCode(JsonNode response, int expectedCode) {
        assertThat(response.at("/code").asInt())
                .describedAs(response.toPrettyString())
                .isEqualTo(expectedCode);
    }

    private void addAuthorization(HttpRequest.Builder builder, String token) {
        if (token != null) {
            builder.header("Authorization", bearer(token));
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
