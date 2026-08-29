package moon.yukiss.service;

import moon.yukiss.common.PageResult;
import moon.yukiss.dto.ArticleRequest;
import moon.yukiss.entity.Article;

import java.util.List;

public interface ArticleService {
    List<Article> list();

    PageResult<Article> page(Integer page, Integer pageSize, String keyword, String sort);

    List<Article> listMine();

    Article getById(Integer id);

    Article add(ArticleRequest request);

    Article update(Integer id, ArticleRequest request);

    void delete(Integer id);
}
