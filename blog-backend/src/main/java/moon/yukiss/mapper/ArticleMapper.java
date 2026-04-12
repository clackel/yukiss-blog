package moon.yukiss.mapper;

import moon.yukiss.entity.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;



@Mapper
public interface ArticleMapper {

    // 1. 获取所有文章列表
    @Select("SELECT * FROM article ORDER BY create_time DESC")
    List<Article> list(); // 统一叫 list，干净利落

    // 2. 根据 ID 获取单篇文章详情
    @Select("SELECT * FROM article WHERE id = #{id}")
    Article getById(Integer id);

    @Insert("INSERT INTO article (title, content, create_time) VALUES (#{title}, #{content}, NOW())")
    int insert(Article article);

}