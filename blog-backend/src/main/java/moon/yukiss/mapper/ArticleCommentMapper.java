package moon.yukiss.mapper;

import moon.yukiss.entity.ArticleComment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ArticleCommentMapper {

    // 发送评论
    @Insert("INSERT INTO article_comment(content, article_id, user_id, parent_id, create_time) " +
            "VALUES(#{content}, #{articleId}, #{userId}, #{parentId}, NOW())")
    void insert(ArticleComment comment);

    // 查询某篇文章下的所有评论
    @Select("SELECT c.*, u.nickname, u.avatar FROM article_comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.article_id = #{articleId} ORDER BY c.create_time DESC")
    List<ArticleComment> findByArticleId(Integer articleId);
}