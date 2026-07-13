package com.tyj.campuscircle.notice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.NoticeEntity;
import com.tyj.campuscircle.post.PageQueryResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Map;

public interface NoticeMapper extends BaseMapper<NoticeEntity> {

    default void saveNotice(CreateNoticeCommand command) {
        try {
            insertNotice(command);
        } catch (DuplicateKeyException ignored) {
            // Idempotent event consumption: the same comment/like event may be delivered more than once.
        }
    }

    @Insert("""
            INSERT INTO notice (receiver_id, sender_id, post_id, comment_id, type, event_key, content)
            VALUES (#{command.receiverId}, #{command.senderId}, #{command.postId}, #{command.commentId},
                    #{command.type}, #{command.eventKey}, #{command.content})
            """)
    void insertNotice(@Param("command") CreateNoticeCommand command);

    @SelectProvider(type = NoticeSqlProvider.class, method = "countNotices")
    long countNotices(@Param("receiverId") Long receiverId, @Param("readStatus") Integer readStatus);

    @SelectProvider(type = NoticeSqlProvider.class, method = "selectNotices")
    List<NoticeItem> selectNotices(@Param("receiverId") Long receiverId,
                                   @Param("readStatus") Integer readStatus,
                                   @Param("limit") int limit,
                                   @Param("offset") int offset);

    default PageQueryResult<NoticeItem> findNoticesByReceiverId(Long receiverId, int page, int size, Integer readStatus) {
        long total = countNotices(receiverId, readStatus);
        List<NoticeItem> records = selectNotices(receiverId, readStatus, size, (page - 1) * size);
        return new PageQueryResult<>(total, records);
    }

    @Select("SELECT COUNT(*) FROM notice WHERE receiver_id = #{receiverId} AND read_status = 0")
    long countUnreadByReceiverId(@Param("receiverId") Long receiverId);

    @Update("UPDATE notice SET read_status = 1 WHERE id = #{noticeId} AND receiver_id = #{receiverId} AND read_status = 0")
    int markRead(@Param("noticeId") Long noticeId, @Param("receiverId") Long receiverId);

    @Update("UPDATE notice SET read_status = 1 WHERE receiver_id = #{receiverId} AND read_status = 0")
    int markAllRead(@Param("receiverId") Long receiverId);

    @Select("SELECT COUNT(*) FROM notice WHERE id = #{noticeId} AND receiver_id = #{receiverId}")
    long countByIdAndReceiverId(@Param("noticeId") Long noticeId, @Param("receiverId") Long receiverId);

    default boolean existsByIdAndReceiverId(Long noticeId, Long receiverId) {
        return countByIdAndReceiverId(noticeId, receiverId) > 0;
    }

    class NoticeSqlProvider {
        public String countNotices(Map<String, Object> params) {
            return """
                    SELECT COUNT(*)
                    FROM notice n
                    """ + whereClause(params);
        }

        public String selectNotices(Map<String, Object> params) {
            return """
                    SELECT n.id, n.receiver_id AS receiverId, n.sender_id AS senderId,
                           u.nickname AS senderNickname, u.avatar_url AS senderAvatarUrl,
                           n.post_id AS postId, n.comment_id AS commentId, n.type, n.content,
                           n.read_status AS readStatus, n.created_at AS createdAt
                    FROM notice n
                    JOIN `user` u ON n.sender_id = u.id
                    """ + whereClause(params) + """
                     ORDER BY n.created_at DESC, n.id DESC
                     LIMIT #{limit} OFFSET #{offset}
                    """;
        }

        private String whereClause(Map<String, Object> params) {
            StringBuilder where = new StringBuilder(" WHERE n.receiver_id = #{receiverId}");
            if (params.get("readStatus") != null) {
                where.append(" AND n.read_status = #{readStatus}");
            }
            return where.toString();
        }
    }
}
