package moon.yukiss.mapper;

import moon.yukiss.dto.TagCount;
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
            @Param("currentUserId") Integer currentUserId,
            @Param("includeDrafts") boolean includeDrafts
    );

    /** 不区分状态，可见性由 service 判断 */
    Article getById(
            @Param("id") Integer id,
            @Param("currentUserId") Integer currentUserId
    );

    long countPage(
            @Param("likeKeyword") String likeKeyword,
            @Param("fulltextKeyword") String fulltextKeyword,
            @Param("tag") String tag
    );

    List<Article> page(
            @Param("currentUserId") Integer currentUserId,
            @Param("likeKeyword") String likeKeyword,
            @Param("fulltextKeyword") String fulltextKeyword,
            @Param("tag") String tag,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 只取权限判断需要的字段：id、author_id、status */
    @Select("SELECT id, author_id, status FROM article WHERE id = #{id}")
    Article findBasicById(@Param("id") Integer id);

    @Insert("""
            INSERT INTO article (author_id, title, content, status, create_time, update_time)
            VALUES (#{authorId}, #{title}, #{content}, #{status}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    /** 草稿首次发布时把 create_time 更新为发布时间（必须写在 status 赋值之前） */
    @Update("""
            UPDATE article
            SET create_time = CASE WHEN status = 'draft' AND #{status} = 'published' THEN NOW() ELSE create_time END,
                title = #{title}, content = #{content}, status = #{status}, update_time = NOW()
            WHERE id = #{id}
            """)
    int update(Article article);

    // ---- 冗余计数：每次从源表重新统计，避免并发下增减出错 ----

    @Update("""
            UPDATE article
            SET like_count = (SELECT COUNT(*) FROM article_like WHERE article_id = #{id})
            WHERE id = #{id}
            """)
    int refreshLikeCount(@Param("id") Integer id);

    @Update("""
            UPDATE article
            SET comment_count = (SELECT COUNT(*) FROM article_comment WHERE article_id = #{id}),
                last_comment_time = (SELECT MAX(create_time) FROM article_comment WHERE article_id = #{id})
            WHERE id = #{id}
            """)
    int refreshCommentStats(@Param("id") Integer id);

    @Select("SELECT like_count FROM article WHERE id = #{id}")
    int selectLikeCount(@Param("id") Integer id);

    // ---- 标签 ----

    int insertTags(@Param("articleId") Integer articleId, @Param("tags") List<String> tags);

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    int deleteTagsByArticleId(@Param("articleId") Integer articleId);

    List<TagCount> listPopularTags(@Param("limit") int limit);

    // ---- 删除文章时的级联清理 ----

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

    @Delete("DELETE FROM notification WHERE article_id = #{articleId}")
    int deleteNotificationsByArticleId(@Param("articleId") Integer articleId);

    @Delete("DELETE FROM article WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
}
