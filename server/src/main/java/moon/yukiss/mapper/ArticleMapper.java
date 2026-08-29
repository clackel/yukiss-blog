package moon.yukiss.mapper;

import moon.yukiss.entity.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ArticleMapper {
    String ARTICLE_SELECT = """
            SELECT a.*, u.nickname AS authorNickname, u.avatar AS authorAvatar,
                   (SELECT COUNT(*) FROM article_like al WHERE al.article_id = a.id) AS likeCount,
                   (SELECT COUNT(*) FROM article_comment ac WHERE ac.article_id = a.id) AS commentCount,
                   EXISTS(
                       SELECT 1 FROM article_like al2
                       WHERE al2.article_id = a.id AND al2.user_id = #{currentUserId}
                   ) AS likedByMe
            FROM article a
            LEFT JOIN user u ON a.author_id = u.id
            """;

    @Select(ARTICLE_SELECT + " ORDER BY a.create_time DESC")
    List<Article> list(@Param("currentUserId") Integer currentUserId);

    @Select(ARTICLE_SELECT + """
            WHERE a.author_id = #{authorId}
            ORDER BY a.create_time DESC
            """)
    List<Article> listByAuthor(
            @Param("authorId") Integer authorId,
            @Param("currentUserId") Integer currentUserId
    );

    @Select(ARTICLE_SELECT + " WHERE a.id = #{id}")
    Article getById(
            @Param("id") Integer id,
            @Param("currentUserId") Integer currentUserId
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM article a
            LEFT JOIN user u ON a.author_id = u.id
            <where>
                <if test="keyword != null and keyword != ''">
                    (
                        a.title LIKE CONCAT('%', #{keyword}, '%')
                        OR a.content LIKE CONCAT('%', #{keyword}, '%')
                        OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                    )
                </if>
            </where>
            </script>
            """)
    long countPage(@Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT a.*, u.nickname AS authorNickname, u.avatar AS authorAvatar,
                   (SELECT COUNT(*) FROM article_like al WHERE al.article_id = a.id) AS likeCount,
                   (SELECT COUNT(*) FROM article_comment ac WHERE ac.article_id = a.id) AS commentCount,
                   EXISTS(
                       SELECT 1 FROM article_like al2
                       WHERE al2.article_id = a.id AND al2.user_id = #{currentUserId}
                   ) AS likedByMe
            FROM article a
            LEFT JOIN user u ON a.author_id = u.id
            <where>
                <if test="keyword != null and keyword != ''">
                    (
                        a.title LIKE CONCAT('%', #{keyword}, '%')
                        OR a.content LIKE CONCAT('%', #{keyword}, '%')
                        OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                    )
                </if>
            </where>
            <choose>
                <when test="sort == 'popular'">
                    ORDER BY likeCount DESC, commentCount DESC, a.create_time DESC
                </when>
                <otherwise>
                    ORDER BY a.create_time DESC
                </otherwise>
            </choose>
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Article> page(
            @Param("currentUserId") Integer currentUserId,
            @Param("keyword") String keyword,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM article WHERE id = #{id}")
    int existsById(@Param("id") Integer id);

    @Insert("""
            INSERT INTO article (author_id, title, content, create_time, update_time)
            VALUES (#{authorId}, #{title}, #{content}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @Update("""
            UPDATE article
            SET title = #{title}, content = #{content}, update_time = NOW()
            WHERE id = #{id}
            """)
    int update(Article article);

    @Delete("""
            DELETE FROM comment_like
            WHERE comment_id IN (
                SELECT id FROM article_comment WHERE article_id = #{articleId}
            )
            """)
    int deleteCommentLikesByArticleId(@Param("articleId") Integer articleId);

    @Update("UPDATE article_comment SET parent_id = NULL WHERE article_id = #{articleId}")
    int detachCommentRepliesByArticleId(@Param("articleId") Integer articleId);

    @Delete("DELETE FROM article_comment WHERE article_id = #{articleId}")
    int deleteCommentsByArticleId(@Param("articleId") Integer articleId);

    @Delete("DELETE FROM article_like WHERE article_id = #{articleId}")
    int deleteLikesByArticleId(@Param("articleId") Integer articleId);

    @Delete("DELETE FROM article WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
}
