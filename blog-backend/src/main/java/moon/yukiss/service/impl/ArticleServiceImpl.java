package moon.yukiss.service.impl;

import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleService;
import moon.yukiss.common.BusinessException;
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
        return articleMapper.list(currentUserIdOrNull());
    }

    @Override
    public List<Article> listMine() {
        Integer userId = requireCurrentUserId();
        return articleMapper.listByAuthor(userId, userId);
    }

    @Override
    public Article getById(Integer id) {
        return articleMapper.getById(id, currentUserIdOrNull());
    }

    @Override
    public void add(Article article) {
        article.setAuthorId(requireCurrentUserId());
        articleMapper.insert(article);
    }

    private Integer currentUserIdOrNull() {
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map == null) {
            return null;
        }
        Object id = map.get("id");
        return id instanceof Number ? ((Number) id).intValue() : null;
    }

    private Integer requireCurrentUserId() {
        Integer userId = currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }
}
