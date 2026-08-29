package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.ArticleRequest;
import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleService;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ArticleServiceImpl implements ArticleService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 80;
    private static final Set<String> ALLOWED_SORTS = Set.of("published", "commented", "likes", "comments");

    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public List<Article> list() {
        return articleMapper.list(currentUserIdOrNull());
    }

    @Override
    public PageResult<Article> page(Integer page, Integer pageSize, String keyword, String sort) {
        int normalizedPage = page == null ? 1 : page;
        int normalizedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalizedPage < 1) {
            throw new BusinessException("页码必须大于等于 1");
        }
        if (normalizedPageSize < 1 || normalizedPageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量必须在 1 到 50 之间");
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSort = normalizeSort(sort);
        long total = articleMapper.countPage(normalizedKeyword);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<Article> items = total == 0
                ? List.of()
                : articleMapper.page(
                        currentUserIdOrNull(),
                        normalizedKeyword,
                        normalizedSort,
                        offset,
                        normalizedPageSize
                );
        return PageResult.of(items, total, normalizedPage, normalizedPageSize);
    }

    @Override
    public List<Article> listMine() {
        Integer userId = requireCurrentUserId();
        return articleMapper.listByAuthor(userId, userId);
    }

    @Override
    public List<Article> listByAuthor(Integer authorId) {
        if (authorId == null || authorId < 1) {
            throw BusinessException.notFound("用户不存在");
        }
        return articleMapper.listByAuthor(authorId, currentUserIdOrNull());
    }

    @Override
    public Article getById(Integer id) {
        if (id == null || id < 1) {
            throw BusinessException.notFound("文章不存在");
        }
        Article article = articleMapper.getById(id, currentUserIdOrNull());
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        return article;
    }

    @Override
    public Article add(ArticleRequest request) {
        Integer userId = requireCurrentUserId();
        Article article = new Article();
        article.setAuthorId(userId);
        applyRequest(article, request);
        articleMapper.insert(article);
        return articleMapper.getById(article.getId(), userId);
    }

    @Override
    public Article update(Integer id, ArticleRequest request) {
        Integer userId = requireCurrentUserId();
        Article article = requireOwnedArticle(id, userId);
        applyRequest(article, request);
        articleMapper.update(article);
        return articleMapper.getById(id, userId);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Integer userId = requireCurrentUserId();
        requireOwnedArticle(id, userId);
        articleMapper.deleteCommentLikesByArticleId(id);
        articleMapper.detachCommentRepliesByArticleId(id);
        articleMapper.deleteCommentsByArticleId(id);
        articleMapper.deleteLikesByArticleId(id);
        articleMapper.deleteById(id);
    }

    private Article requireOwnedArticle(Integer id, Integer userId) {
        if (id == null || id < 1) {
            throw BusinessException.notFound("文章不存在");
        }
        Article article = articleMapper.getById(id, userId);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        if (!userId.equals(article.getAuthorId())) {
            throw BusinessException.forbidden("只能修改或删除自己的文章");
        }
        return article;
    }

    private void applyRequest(Article article, ArticleRequest request) {
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent().trim());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException("搜索关键词不能超过 80 个字符");
        }
        return normalized;
    }

    private String normalizeSort(String sort) {
        String normalized = sort == null || sort.isBlank() ? "published" : sort.trim().toLowerCase();
        if ("latest".equals(normalized)) {
            normalized = "published";
        } else if ("popular".equals(normalized)) {
            normalized = "likes";
        }
        if (!ALLOWED_SORTS.contains(normalized)) {
            throw new BusinessException("排序方式仅支持 published、commented、likes 或 comments");
        }
        return normalized;
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
            throw BusinessException.unauthorized("请先登录");
        }
        return userId;
    }
}
