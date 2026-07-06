package moon.yukiss.service;

import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public List<Article> list() {
        return articleMapper.list(currentUserId());
    }

    @Override
    public List<Article> listMine() {
        Integer userId = currentUserId();
        return articleMapper.listByAuthor(userId, userId);
    }

    @Override
    public Article getById(Integer id) {
        return articleMapper.getById(id, currentUserId());
    }

    @Override
    public void add(Article article) {
        article.setAuthorId(currentUserId());
        articleMapper.insert(article);
    }

    private Integer currentUserId() {
        Map<String,Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }
}
