package moon.yukiss.mapper;

import moon.yukiss.entity.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;



@Mapper
public interface ArticleMapper {

    // 1. 获取所有文章列表
    @Select("SELECT a.*, u.nickname AS authorNickname, u.avatar AS authorAvatar " +
            "FROM article a " +
            "LEFT JOIN user u ON a.author_id = u.id " +
            "ORDER BY a.create_time DESC")
    List<Article> list();

    // 2. 根据 ID 获取单篇文章详情
    @Select("SELECT * FROM article WHERE id = #{id}")
    Article getById(Integer id);

    // 3. 发布文章
    @Insert("INSERT INTO article (author_id, title, content, create_time, update_time) " +
            "VALUES (#{authorId}, #{title}, #{content}, NOW(), NOW())")
    int insert(Article article);

}