package moon.yukiss.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleBookmarkMapper {
    @Select("SELECT COUNT(*) FROM article_bookmark WHERE user_id = #{userId} AND article_id = #{articleId}")
    int checkIsBookmarked(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /** INSERT IGNORE：并发重复收藏时返回 0 而不是触发唯一键异常 */
    @Insert("INSERT IGNORE INTO article_bookmark(user_id, article_id, create_time) VALUES(#{userId}, #{articleId}, NOW())")
    int addBookmark(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    @Delete("DELETE FROM article_bookmark WHERE user_id = #{userId} AND article_id = #{articleId}")
    int deleteBookmark(@Param("userId") Integer userId, @Param("articleId") Integer articleId);
}
