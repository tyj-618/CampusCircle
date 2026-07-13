package com.tyj.campuscircle.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.PostEntity;
import com.tyj.campuscircle.common.entity.PostStatEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PostMapper extends BaseMapper<PostEntity> {

    @Select("SELECT COUNT(*) FROM category WHERE id = #{categoryId} AND status = 0")
    long countEnabledCategory(@Param("categoryId") Long categoryId);

    default boolean existsEnabledCategory(Long categoryId) {
        return countEnabledCategory(categoryId) > 0;
    }

    default Long savePost(Long userId, CreatePostRequest request) {
        PostEntity post = new PostEntity();
        post.setUserId(userId);
        post.setCategoryId(request.categoryId());
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        insert(post);
        return post.getId();
    }

    @Insert("INSERT INTO post_stat (post_id) VALUES (#{postId})")
    void savePostStat(@Param("postId") Long postId);

    @Select("""
            SELECT p.id, p.user_id AS userId, p.category_id AS categoryId, p.title, p.content, p.status,
                   p.created_at AS createdAt, p.updated_at AS updatedAt,
                   c.name AS categoryName, c.code AS categoryCode,
                   u.nickname AS authorNickname, u.avatar_url AS authorAvatarUrl, u.role AS authorRole,
                   ps.view_count AS viewCount, ps.like_count AS likeCount,
                   ps.comment_count AS commentCount, ps.hot_score AS hotScore
            FROM post p
            JOIN category c ON p.category_id = c.id
            JOIN `user` u ON p.user_id = u.id
            JOIN post_stat ps ON p.id = ps.post_id
            WHERE p.id = #{postId}
            """)
    PostDetail selectDetailById(@Param("postId") Long postId);

    default Optional<PostDetail> findDetailById(Long postId) {
        return Optional.ofNullable(selectDetailById(postId));
    }

    @Update("""
            UPDATE post_stat
            SET view_count = view_count + 1,
                hot_score = hot_score + 1
            WHERE post_id = #{postId}
            """)
    void increaseViewCount(@Param("postId") Long postId);

    @SelectProvider(type = PostSqlProvider.class, method = "countPosts")
    long countPosts(@Param("categoryId") Long categoryId,
                    @Param("keyword") String keyword,
                    @Param("userId") Long userId);

    @SelectProvider(type = PostSqlProvider.class, method = "findPosts")
    List<PostListItem> selectPosts(@Param("limit") int limit,
                                   @Param("offset") int offset,
                                   @Param("categoryId") Long categoryId,
                                   @Param("keyword") String keyword,
                                   @Param("userId") Long userId,
                                   @Param("sort") String sort,
                                   @Param("ids") List<Long> ids);

    default PageQueryResult<PostListItem> findPosts(int page, int size, Long categoryId, String keyword, String sort) {
        long total = countPosts(categoryId, keyword, null);
        List<PostListItem> records = selectPosts(size, (page - 1) * size, categoryId, keyword, null, sort, null);
        return new PageQueryResult<>(total, records);
    }

    default PageQueryResult<PostListItem> findPostsByUserId(Long userId, int page, int size) {
        long total = countPosts(null, null, userId);
        List<PostListItem> records = selectPosts(size, (page - 1) * size, null, null, userId, null, null);
        return new PageQueryResult<>(total, records);
    }

    default List<PostListItem> findHotPosts(int limit, Long categoryId) {
        return selectPosts(limit, 0, categoryId, null, null, "hot", null);
    }

    default List<PostListItem> findHotPostsByIds(List<Long> postIds, Long categoryId) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            order.put(postIds.get(i), i);
        }
        return selectPosts(postIds.size(), 0, categoryId, null, null, "hot", postIds).stream()
                .sorted(Comparator.comparingInt(item -> order.getOrDefault(item.id(), Integer.MAX_VALUE)))
                .toList();
    }

    @Update("""
            UPDATE post
            SET category_id = #{request.categoryId}, title = #{request.title}, content = #{request.content}
            WHERE id = #{postId}
            """)
    void updatePostRaw(@Param("postId") Long postId, @Param("request") UpdatePostRequest request);

    default void updatePost(Long postId, UpdatePostRequest request) {
        updatePostRaw(postId, new UpdatePostRequest(
                request.categoryId(),
                request.title().trim(),
                request.content().trim()
        ));
    }

    @Update("UPDATE post SET status = 1 WHERE id = #{postId}")
    void softDeletePost(@Param("postId") Long postId);

    @Select("""
            SELECT COUNT(*)
            FROM post_like
            WHERE post_id = #{postId} AND user_id = #{userId} AND status = 0
            """)
    long countLike(@Param("postId") Long postId, @Param("userId") Long userId);

    default boolean existsLike(Long postId, Long userId) {
        return countLike(postId, userId) > 0;
    }

    class PostSqlProvider {
        public String countPosts(Map<String, Object> params) {
            return """
                    <script>
                    SELECT COUNT(*)
                    FROM post p
                    JOIN category c ON p.category_id = c.id
                    JOIN `user` u ON p.user_id = u.id
                    JOIN post_stat ps ON p.id = ps.post_id
                    """ + whereClause(params) + """
                    </script>
                    """;
        }

        public String findPosts(Map<String, Object> params) {
            String orderBy = "hot".equalsIgnoreCase((String) params.get("sort"))
                    ? "ps.hot_score DESC, p.created_at DESC"
                    : "p.created_at DESC";
            return """
                    <script>
                    SELECT p.id, p.title, p.content, p.category_id AS categoryId,
                           c.name AS categoryName, c.code AS categoryCode,
                           u.id AS authorId, u.nickname AS authorNickname, u.avatar_url AS authorAvatarUrl,
                           ps.view_count AS viewCount, ps.like_count AS likeCount,
                           ps.comment_count AS commentCount, ps.hot_score AS hotScore,
                           p.created_at AS createdAt
                    FROM post p
                    JOIN category c ON p.category_id = c.id
                    JOIN `user` u ON p.user_id = u.id
                    JOIN post_stat ps ON p.id = ps.post_id
                    """ + whereClause(params) + """
                     ORDER BY """ + orderBy + """
                     LIMIT #{limit} OFFSET #{offset}
                    </script>
                    """;
        }

        private String whereClause(Map<String, Object> params) {
            StringBuilder where = new StringBuilder(" WHERE p.status = 0");
            if (params.get("categoryId") != null) {
                where.append(" AND p.category_id = #{categoryId}");
            }
            String keyword = (String) params.get("keyword");
            if (keyword != null && !keyword.isBlank()) {
                where.append(" AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%'))");
            }
            if (params.get("userId") != null) {
                where.append(" AND p.user_id = #{userId}");
            }
            if (params.get("ids") instanceof List<?> ids && !ids.isEmpty()) {
                where.append(" AND p.id IN ");
                where.append("<foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach>");
            }
            return where.toString();
        }
    }
}
