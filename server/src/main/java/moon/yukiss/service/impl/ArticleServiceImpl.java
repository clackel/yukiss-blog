package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.ArticleRequest;
import moon.yukiss.dto.TagCount;
import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleService;
import moon.yukiss.utils.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ArticleServiceImpl implements ArticleService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 80;
    static final int MAX_TAGS = 5;
    static final int MAX_TAG_LENGTH = 20;
    private static final int POPULAR_TAG_LIMIT = 20;
    private static final Set<String> ALLOWED_SORTS = Set.of("published", "commented", "likes", "comments");
    private static final Set<String> ALLOWED_STATUSES = Set.of(Article.STATUS_PUBLISHED, Article.STATUS_DRAFT);

    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public List<Article> list() {
        return articleMapper.list(CurrentUser.idOrNull());
    }

    @Override
    public PageResult<Article> page(Integer page, Integer pageSize, String keyword, String sort, String tag) {
        int normalizedPage = page == null ? 1 : page;
        int normalizedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalizedPage < 1) {
            throw new BusinessException("页码必须大于等于 1");
        }
        if (normalizedPageSize < 1 || normalizedPageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量必须在 1 到 50 之间");
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        String likeKeyword = normalizedKeyword == null ? null : escapeLike(normalizedKeyword);
        String fulltextKeyword = normalizedKeyword == null ? null : toFulltextPhrase(normalizedKeyword);
        String normalizedTag = tag == null || tag.isBlank() ? null : normalizeTag(tag);
        String normalizedSort = normalizeSort(sort);

        long total = articleMapper.countPage(likeKeyword, fulltextKeyword, normalizedTag);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<Article> items = total == 0
                ? List.of()
                : articleMapper.page(
                        CurrentUser.idOrNull(),
                        likeKeyword,
                        fulltextKeyword,
                        normalizedTag,
                        normalizedSort,
                        offset,
                        normalizedPageSize
                );
        return PageResult.of(items, total, normalizedPage, normalizedPageSize);
    }

    @Override
    public List<Article> listMine() {
        Integer userId = CurrentUser.requireId();
        return articleMapper.listByAuthor(userId, userId, true);
    }

    @Override
    public List<Article> listByAuthor(Integer authorId) {
        if (authorId == null || authorId < 1) {
            throw BusinessException.notFound("用户不存在");
        }
        return articleMapper.listByAuthor(authorId, CurrentUser.idOrNull(), false);
    }

    @Override
    public Article getById(Integer id) {
        if (id == null || id < 1) {
            throw BusinessException.notFound("文章不存在");
        }
        Integer currentUserId = CurrentUser.idOrNull();
        Article article = articleMapper.getById(id, currentUserId);
        // 草稿对作者以外的人表现为不存在
        if (article == null || (article.isDraft() && !article.getAuthorId().equals(currentUserId))) {
            throw BusinessException.notFound("文章不存在");
        }
        return article;
    }

    @Override
    @Transactional
    public Article add(ArticleRequest request) {
        Integer userId = CurrentUser.requireId();
        Article article = new Article();
        article.setAuthorId(userId);
        List<String> tags = applyRequest(article, request);
        articleMapper.insert(article);
        replaceTags(article.getId(), tags);
        return articleMapper.getById(article.getId(), userId);
    }

    @Override
    @Transactional
    public Article update(Integer id, ArticleRequest request) {
        Integer userId = CurrentUser.requireId();
        Article article = requireOwnedArticle(id, userId);
        List<String> tags = applyRequest(article, request);
        articleMapper.update(article);
        replaceTags(id, tags);
        return articleMapper.getById(id, userId);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Integer userId = CurrentUser.requireId();
        requireOwnedArticle(id, userId);
        articleMapper.deleteCommentLikesByArticleId(id);
        articleMapper.detachCommentRepliesByArticleId(id);
        articleMapper.deleteCommentsByArticleId(id);
        articleMapper.deleteLikesByArticleId(id);
        articleMapper.deleteTagsByArticleId(id);
        articleMapper.deleteNotificationsByArticleId(id);
        articleMapper.deleteById(id);
    }

    @Override
    public List<TagCount> listPopularTags() {
        return articleMapper.listPopularTags(POPULAR_TAG_LIMIT);
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

    /** 写入标题、正文、状态，返回规范化后的标签 */
    private List<String> applyRequest(Article article, ArticleRequest request) {
        String status = normalizeStatus(request.getStatus());
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (Article.STATUS_PUBLISHED.equals(status) && content.isEmpty()) {
            throw new BusinessException("请填写文章正文");
        }
        article.setTitle(request.getTitle().trim());
        article.setContent(content);
        article.setStatus(status);
        return normalizeTags(request.getTags());
    }

    private void replaceTags(Integer articleId, List<String> tags) {
        articleMapper.deleteTagsByArticleId(articleId);
        if (!tags.isEmpty()) {
            articleMapper.insertTags(articleId, tags);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return Article.STATUS_PUBLISHED;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException("文章状态仅支持 published 或 draft");
        }
        return normalized;
    }

    /** 去掉首尾空白和开头的 #，忽略空标签，按不区分大小写去重并保留首次出现的写法 */
    static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String normalized = normalizeTag(tag);
            unique.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
        }
        if (unique.size() > MAX_TAGS) {
            throw new BusinessException("每篇文章最多 " + MAX_TAGS + " 个标签");
        }
        return new ArrayList<>(unique.values());
    }

    private static String normalizeTag(String tag) {
        String normalized = tag.trim();
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.isEmpty()) {
            throw new BusinessException("标签不能为空");
        }
        if (normalized.length() > MAX_TAG_LENGTH) {
            throw new BusinessException("标签不能超过 " + MAX_TAG_LENGTH + " 个字符");
        }
        if (normalized.contains(",")) {
            throw new BusinessException("标签不能包含逗号");
        }
        return normalized;
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

    /** 转义 LIKE 通配符，使 % 和 _ 按字面匹配（MySQL 默认转义符为反斜杠） */
    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 生成全文检索条件，只用于缩小候选范围，最终仍由 LIKE 精确匹配（见 ArticleMapper.xml）。
     * 关键词按非字母数字字符拆成片段，要求每个片段都出现（+"片段"），得到的候选集一定包含所有真正匹配的文章。
     * ngram 的最小单位是 2 个字符，任一片段不足 2 个字符时无法用索引，返回 null 直接用 LIKE。
     */
    static String toFulltextPhrase(String keyword) {
        List<String> terms = new ArrayList<>();
        for (String fragment : keyword.split("[^\\p{L}\\p{N}]+")) {
            if (fragment.isEmpty()) {
                continue;
            }
            if (fragment.codePointCount(0, fragment.length()) < 2) {
                return null;
            }
            terms.add("+\"" + fragment + "\"");
        }
        return terms.isEmpty() ? null : String.join(" ", terms);
    }

    private String normalizeSort(String sort) {
        String normalized = sort == null || sort.isBlank() ? "published" : sort.trim().toLowerCase(Locale.ROOT);
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
}
