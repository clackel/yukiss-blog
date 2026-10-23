package moon.yukiss.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper {
    // 查询是否点赞
    @Select("SELECT COUNT(*) FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int checkIsLiked(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    // 点赞；INSERT IGNORE 使并发重复点赞返回 0 而不是触发唯一键异常
    @Insert("INSERT IGNORE INTO article_like(user_id, article_id, create_time) VALUES(#{userId}, #{articleId}, NOW())")
    int addLike(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    // 取消点赞
    @Delete("DELETE FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int deleteLike(@Param("userId") Integer userId, @Param("articleId") Integer articleId);
}
