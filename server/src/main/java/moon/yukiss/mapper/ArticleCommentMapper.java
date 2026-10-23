package moon.yukiss.mapper;

import moon.yukiss.entity.ArticleComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ArticleCommentMapper {
    String COMMENT_SELECT = """
            SELECT c.*,
                   (u.id IS NULL OR u.is_deleted = 1) AS userDeleted,
                   CASE WHEN u.id IS NULL OR u.is_deleted = 1 THEN '已注销用户' ELSE u.nickname END AS nickname,
                   CASE WHEN u.id IS NULL OR u.is_deleted = 1 THEN NULL ELSE u.avatar END AS avatar,
                   CASE WHEN pu.is_deleted = 1 THEN '已注销用户' ELSE pu.nickname END AS parentNickname,
                   (SELECT COUNT(*) FROM comment_like cl WHERE cl.comment_id = c.id) AS likeCount,
                   EXISTS(
                       SELECT 1 FROM comment_like cl2
                       WHERE cl2.comment_id = c.id AND cl2.user_id = #{currentUserId}
                   ) AS likedByMe
            FROM article_comment c
            LEFT JOIN user u ON c.user_id = u.id
            LEFT JOIN article_comment pc ON c.parent_id = pc.id
            LEFT JOIN user pu ON pc.user_id = pu.id
            """;

    @Insert("""
            INSERT INTO article_comment(content, article_id, user_id, parent_id, create_time)
            VALUES(#{content}, #{articleId}, #{userId}, #{parentId}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleComment comment);

    @Select(COMMENT_SELECT + " WHERE c.id = #{id}")
    ArticleComment findById(
            @Param("id") Integer id,
            @Param("currentUserId") Integer currentUserId
    );

    /** 一级评论分页，按时间正序 */
    @Select(COMMENT_SELECT + """
            WHERE c.article_id = #{articleId} AND c.parent_id IS NULL
            ORDER BY c.create_time ASC, c.id ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ArticleComment> pageTopLevel(
            @Param("articleId") Integer articleId,
            @Param("currentUserId") Integer currentUserId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM article_comment WHERE article_id = #{articleId} AND parent_id IS NULL")
    long countTopLevel(@Param("articleId") Integer articleId);

    @Select("<script>" + COMMENT_SELECT + """
            WHERE c.parent_id IN
            <foreach collection="parentIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY c.create_time ASC, c.id ASC
            </script>
            """)
    List<ArticleComment> findRepliesByParentIds(
            @Param("parentIds") List<Integer> parentIds,
            @Param("currentUserId") Integer currentUserId
    );

    @Select("SELECT id FROM article_comment WHERE parent_id = #{parentId}")
    List<Integer> findReplyIds(@Param("parentId") Integer parentId);

    @Update("UPDATE article_comment SET content = #{content}, update_time = NOW() WHERE id = #{id}")
    int updateContent(@Param("id") Integer id, @Param("content") String content);

    @Delete("""
            <script>
            DELETE FROM comment_like WHERE comment_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    int deleteLikesByCommentIds(@Param("ids") List<Integer> ids);

    @Delete("""
            <script>
            DELETE FROM article_comment WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    int deleteByIds(@Param("ids") List<Integer> ids);

    @Select("SELECT COUNT(*) FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId}")
    int checkCommentLiked(
            @Param("userId") Integer userId,
            @Param("commentId") Integer commentId
    );

    /** INSERT IGNORE：并发重复点赞时返回 0 而不是触发唯一键异常 */
    @Insert("INSERT IGNORE INTO comment_like(user_id, comment_id, create_time) VALUES(#{userId}, #{commentId}, NOW())")
    int addCommentLike(
            @Param("userId") Integer userId,
            @Param("commentId") Integer commentId
    );

    @Delete("DELETE FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId}")
    int deleteCommentLike(
            @Param("userId") Integer userId,
            @Param("commentId") Integer commentId
    );

    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId}")
    int countCommentLikes(@Param("commentId") Integer commentId);
}
