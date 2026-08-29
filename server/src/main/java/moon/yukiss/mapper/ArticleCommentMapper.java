package moon.yukiss.mapper;

import moon.yukiss.entity.ArticleComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleCommentMapper {
    String COMMENT_SELECT = """
            SELECT c.*, u.nickname, u.avatar, pu.nickname AS parentNickname,
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

    @Select(COMMENT_SELECT + """
            WHERE c.article_id = #{articleId}
            ORDER BY c.create_time ASC
            """)
    List<ArticleComment> findByArticleId(
            @Param("articleId") Integer articleId,
            @Param("currentUserId") Integer currentUserId
    );

    @Select(COMMENT_SELECT + " WHERE c.id = #{id}")
    ArticleComment findById(
            @Param("id") Integer id,
            @Param("currentUserId") Integer currentUserId
    );

    @Select("SELECT COUNT(*) FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId}")
    int checkCommentLiked(
            @Param("userId") Integer userId,
            @Param("commentId") Integer commentId
    );

    @Insert("INSERT INTO comment_like(user_id, comment_id, create_time) VALUES(#{userId}, #{commentId}, NOW())")
    void addCommentLike(
            @Param("userId") Integer userId,
            @Param("commentId") Integer commentId
    );

    @Delete("DELETE FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId}")
    void deleteCommentLike(
            @Param("userId") Integer userId,
            @Param("commentId") Integer commentId
    );

    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId}")
    int countCommentLikes(@Param("commentId") Integer commentId);
}
