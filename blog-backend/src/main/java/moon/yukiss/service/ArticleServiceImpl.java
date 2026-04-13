package moon.yukiss.service;

import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String,Object> map = ThreadLocalUtil.get();
        // 取出用户的 ID
        Integer authorID = (Integer) map.get("id");
        article.setAuthorId(authorID);
        articleMapper.insert(article);
    }
}
