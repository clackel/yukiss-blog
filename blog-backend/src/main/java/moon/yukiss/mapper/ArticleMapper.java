package moon.yukiss.mapper;

import moon.yukiss.entity.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;



@Mapper
public interface ArticleMapper {

    // 1. 获取所有文章列表
    @Select("SELECT a.*, u.nickname AS authorNickname, u.avatar AS authorAvatar, " +
            "(SELECT COUNT(*) FROM article_like al WHERE al.article_id = a.id) AS likeCount, " +
            "(SELECT COUNT(*) FROM article_comment ac WHERE ac.article_id = a.id) AS commentCount, " +
            "EXISTS(SELECT 1 FROM article_like al2 WHERE al2.article_id = a.id AND al2.user_id = #{currentUserId}) AS likedByMe " +
            "FROM article a " +
            "LEFT JOIN user u ON a.author_id = u.id " +
            "ORDER BY a.create_time DESC")
    List<Article> list(@Param("currentUserId") Integer currentUserId);

    @Select("SELECT a.*, u.nickname AS authorNickname, u.avatar AS authorAvatar, " +
            "(SELECT COUNT(*) FROM article_like al WHERE al.article_id = a.id) AS likeCount, " +
            "(SELECT COUNT(*) FROM article_comment ac WHERE ac.article_id = a.id) AS commentCount, " +
            "EXISTS(SELECT 1 FROM article_like al2 WHERE al2.article_id = a.id AND al2.user_id = #{currentUserId}) AS likedByMe " +
            "FROM article a " +
            "LEFT JOIN user u ON a.author_id = u.id " +
            "WHERE a.author_id = #{authorId} " +
            "ORDER BY a.create_time DESC")
    List<Article> listByAuthor(@Param("authorId") Integer authorId, @Param("currentUserId") Integer currentUserId);

    // 2. 根据 ID 获取单篇文章详情
    @Select("SELECT a.*, u.nickname AS authorNickname, u.avatar AS authorAvatar, " +
            "(SELECT COUNT(*) FROM article_like al WHERE al.article_id = a.id) AS likeCount, " +
            "(SELECT COUNT(*) FROM article_comment ac WHERE ac.article_id = a.id) AS commentCount, " +
            "EXISTS(SELECT 1 FROM article_like al2 WHERE al2.article_id = a.id AND al2.user_id = #{currentUserId}) AS likedByMe " +
            "FROM article a " +
            "LEFT JOIN user u ON a.author_id = u.id " +
            "WHERE a.id = #{id}")
    Article getById(@Param("id") Integer id, @Param("currentUserId") Integer currentUserId);

    // 3. 发布文章
    @Insert("INSERT INTO article (author_id, title, content, create_time, update_time) " +
            "VALUES (#{authorId}, #{title}, #{content}, NOW(), NOW())")
    int insert(Article article);

}
