package moon.yukiss.service;

import moon.yukiss.common.PageResult;
import moon.yukiss.dto.ArticleRequest;
import moon.yukiss.dto.TagCount;
import moon.yukiss.entity.Article;

import java.util.List;

public interface ArticleService {
    List<Article> list();

    /** scope 为 following 时只返回当前用户关注的作者的文章，需要登录 */
    PageResult<Article> page(Integer page, Integer pageSize, String keyword, String sort, String tag, String scope);

    List<Article> listMine();

    List<Article> listByAuthor(Integer authorId);

    Article getById(Integer id);

    Article add(ArticleRequest request);

    Article update(Integer id, ArticleRequest request);

    void delete(Integer id);

    List<TagCount> listPopularTags();
}
