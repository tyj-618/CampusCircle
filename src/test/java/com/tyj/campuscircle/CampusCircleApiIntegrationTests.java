package com.tyj.campuscircle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CampusCircleApiIntegrationTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void campusCircleCoreApiFlow() throws Exception {
        register("alice", "123456", "小艾");
        register("bob", "123456", "小林");

        String aliceToken = login("alice", "123456");
        String bobToken = login("bob", "123456");

        Long categoryId = firstCategoryId();
        Long postId = createPost(aliceToken, categoryId);
        assertLocationFeed(aliceToken, postId);
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
    void campusCircleCoreApiBoundaryFlow() throws Exception {
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

    @Test
    void nearbyFeedSupportsCursorPagination() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = "cursor_" + suffix;
        register(username, "123456", "Cursor User");
        String token = login(username, "123456");
        Long categoryId = firstCategoryId();

        Long firstPostId = createPost(token, categoryId, "Cursor post 1 " + suffix);
        Long secondPostId = createPost(token, categoryId, "Cursor post 2 " + suffix);
        Long thirdPostId = createPost(token, categoryId, "Cursor post 3 " + suffix);

        JsonNode firstPage = get("/api/posts/feed/cursor?radiusKm=30&size=2", token);
        assertThat(firstPage.at("/code").asInt()).isZero();
        assertThat(firstPage.at("/data/records").size()).isEqualTo(2);
        assertThat(firstPage.at("/data/hasMore").asBoolean()).isTrue();
        String nextCursor = firstPage.at("/data/nextCursor").asText();
        assertThat(nextCursor).isNotBlank();

        JsonNode secondPage = get("/api/posts/feed/cursor?radiusKm=30&size=2&cursor=" + nextCursor, token);
        assertThat(secondPage.at("/code").asInt()).isZero();

        Set<Long> firstPageIds = idsOf(firstPage);
        Set<Long> secondPageIds = idsOf(secondPage);
        assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);
        Set<Long> seenIds = new HashSet<>(firstPageIds);
        seenIds.addAll(secondPageIds);
        assertThat(seenIds)
                .contains(firstPostId, secondPostId, thirdPostId);
    }

    @Test
    void aiAssistantUsesOnlyAuthorizedNearbyPosts() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = "assistant_" + suffix;
        String remoteUsername = "remote_assistant_" + suffix;
        String keyword = "studyspace" + suffix;
        register(username, "123456", "Assistant User");
        register(remoteUsername, "123456", "Remote User");
        String token = login(username, "123456");
        String remoteToken = login(remoteUsername, "123456");
        JsonNode remoteProfile = put("/api/users/me", remoteToken, Map.of(
                "nickname", "Remote User",
                "schoolId", 4
        ));
        assertThat(remoteProfile.at("/code").asInt()).isZero();

        Long postId = createPost(token, firstCategoryId(), keyword);
        Long remotePostId = createPost(remoteToken, firstCategoryId(), keyword);
        JsonNode response = post("/api/ai/assistant/ask", token, Map.of(
                "question", keyword,
                "radiusKm", 30
        ));

        assertThat(response.at("/code").asInt()).isZero();
        assertThat(response.at("/data/insufficientEvidence").asBoolean()).isFalse();
        assertThat(response.at("/data/requestId").asText()).isNotBlank();
        assertThat(response.at("/data/references/0/postId").asLong()).isEqualTo(postId);
        for (JsonNode reference : response.at("/data/references")) {
            assertThat(reference.at("/postId").asLong()).isNotEqualTo(remotePostId);
        }

        JsonNode noEvidence = post("/api/ai/assistant/ask", token, Map.of(
                "question", "unmatched" + suffix,
                "radiusKm", 30
        ));
        assertThat(noEvidence.at("/code").asInt()).isZero();
        assertThat(noEvidence.at("/data/insufficientEvidence").asBoolean()).isTrue();
        assertThat(noEvidence.at("/data/references").size()).isZero();

        JsonNode unauthorized = post("/api/ai/assistant/ask", null, Map.of(
                "question", keyword,
                "radiusKm", 30
        ));
        assertCode(unauthorized, 40100);
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
        return createPost(token, categoryId, "高数复习资料怎么整理？");
    }

    private Long createPost(String token, Long categoryId, String title) throws Exception {
        JsonNode response = post("/api/posts", token, Map.of(
                "categoryId", categoryId,
                "title", title,
                "content", "想问问大家期末复习有什么方法。"
        ));

        assertThat(response.at("/code").asInt())
                .describedAs(response.toPrettyString())
                .isZero();
        Long postId = response.at("/data/postId").asLong();
        assertThat(postId).isPositive();
        return postId;
    }

    private Set<Long> idsOf(JsonNode page) {
        Set<Long> ids = new HashSet<>();
        for (JsonNode record : page.at("/data/records")) {
            ids.add(record.at("/id").asLong());
        }
        return ids;
    }

    private void assertLocationFeed(String token, Long postId) throws Exception {
        JsonNode nearbySchools = get("/api/schools/nearby?schoolId=1&radiusKm=30", null);
        assertThat(nearbySchools.at("/code").asInt()).isZero();
        assertThat(nearbySchools.at("/data/0/id").asLong()).isEqualTo(1);

        String schoolName = nearbySchools.at("/data/0/name").asText();
        JsonNode schools = get("/api/schools/search?keyword=" + URLEncoder.encode(schoolName, StandardCharsets.UTF_8), null);
        assertThat(schools.at("/code").asInt()).isZero();
        assertThat(schools.at("/data").size())
                .describedAs(schools.toPrettyString())
                .isGreaterThanOrEqualTo(1);

        JsonNode feed = get("/api/posts/feed?radiusKm=30", token);
        assertThat(feed.at("/code").asInt()).isZero();
        assertThat(feed.at("/data/total").asLong()).isGreaterThanOrEqualTo(1);
        JsonNode matchedPost = null;
        for (JsonNode record : feed.at("/data/records")) {
            if (record.at("/id").asLong() == postId) {
                matchedPost = record;
                break;
            }
        }
        assertThat(matchedPost)
                .describedAs(feed.toPrettyString())
                .isNotNull();
        assertThat(matchedPost.at("/school/id").asLong()).isEqualTo(1);
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
