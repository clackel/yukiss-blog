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
    List<Article> list(@Param("currentUserId") Integer currentUserId);

    List<Article> listByAuthor(
            @Param("authorId") Integer authorId,
            @Param("currentUserId") Integer currentUserId
    );

    Article getById(
            @Param("id") Integer id,
            @Param("currentUserId") Integer currentUserId
    );

    long countPage(@Param("keyword") String keyword);

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
