package moon.yukiss.service;

import moon.yukiss.entity.Article;
import java.util.List;

public interface ArticleService {
    // 获取文章列表，名字和 Mapper 保持一致，叫 list 比较通用
    List<Article> list();

    // 根据 ID 获取单篇文章
    Article getById(Integer id);

    void add(Article article);
}