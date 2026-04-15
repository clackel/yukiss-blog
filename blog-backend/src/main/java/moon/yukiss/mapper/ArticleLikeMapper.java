package moon.yukiss.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper {
    // 查询是否点赞
    @Select("SELECT COUNT(*) FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int checkIsLiked(Integer userId, Integer articleId);

    // 点赞
    @Insert("INSERT INTO article_like(user_id, article_id, create_time) VALUES(#{userId}, #{articleId}, NOW())")
    void addLike(Integer userId, Integer articleId);

    // 取消点赞
    @Delete("DELETE FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    void deleteLike(Integer userId, Integer articleId);
}
