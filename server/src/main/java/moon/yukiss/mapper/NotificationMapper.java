package moon.yukiss.mapper;

import moon.yukiss.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotificationMapper {
    @Insert("""
            INSERT INTO notification (user_id, actor_id, type, article_id, comment_id, is_read, create_time)
            VALUES (#{userId}, #{actorId}, #{type}, #{articleId}, #{commentId}, 0, NOW())
            """)
    int insert(
            @Param("userId") Integer userId,
            @Param("actorId") Integer actorId,
            @Param("type") String type,
            @Param("articleId") Integer articleId,
            @Param("commentId") Integer commentId
    );

    /** 取消点赞 / 取消关注时撤回对应通知（<=> 为 NULL 安全的等于） */
    @Delete("""
            DELETE FROM notification
            WHERE user_id = #{userId} AND actor_id = #{actorId} AND type = #{type}
              AND article_id <=> #{articleId} AND comment_id <=> #{commentId}
            """)
    int deleteMatching(
            @Param("userId") Integer userId,
            @Param("actorId") Integer actorId,
            @Param("type") String type,
            @Param("articleId") Integer articleId,
            @Param("commentId") Integer commentId
    );

    @Delete("""
            <script>
            DELETE FROM notification WHERE comment_id IN
            <foreach collection="commentIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    int deleteByCommentIds(@Param("commentIds") List<Integer> commentIds);

    @Delete("DELETE FROM notification WHERE user_id = #{userId}")
    int deleteByRecipient(@Param("userId") Integer userId);

    @Select("""
            SELECT n.id, n.type, n.actor_id, n.article_id, n.comment_id, n.is_read AS `read`, n.create_time,
                   (u.id IS NULL OR u.is_deleted = 1) AS actorDeleted,
                   CASE WHEN u.id IS NULL OR u.is_deleted = 1 THEN '已注销用户' ELSE u.nickname END AS actorNickname,
                   CASE WHEN u.id IS NULL OR u.is_deleted = 1 THEN NULL ELSE u.avatar END AS actorAvatar,
                   a.title AS articleTitle,
                   c.content AS commentContent
            FROM notification n
            LEFT JOIN user u ON n.actor_id = u.id
            LEFT JOIN article a ON n.article_id = a.id
            LEFT JOIN article_comment c ON n.comment_id = c.id
            WHERE n.user_id = #{userId}
            ORDER BY n.create_time DESC, n.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Notification> page(
            @Param("userId") Integer userId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId}")
    long count(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Integer userId);

    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Integer id, @Param("userId") Integer userId);

    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Integer userId);
}
