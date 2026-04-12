package moon.yukiss.service;

import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public List<Article> list() {
        return articleMapper.list();
    }

    @Override
    public Article getById(Integer id) {
        // 调用 Mapper 里的 getById()
        return articleMapper.getById(id);
    }

    @Override
    public void add(Article article) {
        articleMapper.insert(article);
    }
}
