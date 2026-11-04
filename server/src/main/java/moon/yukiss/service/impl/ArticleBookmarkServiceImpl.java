package moon.yukiss.service.impl;

import moon.yukiss.common.BookmarkResult;
import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleBookmarkMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleBookmarkService;
import moon.yukiss.utils.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleBookmarkServiceImpl implements ArticleBookmarkService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ArticleBookmarkMapper articleBookmarkMapper;
    private final ArticleMapper articleMapper;

    public ArticleBookmarkServiceImpl(ArticleBookmarkMapper articleBookmarkMapper, ArticleMapper articleMapper) {
        this.articleBookmarkMapper = articleBookmarkMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    @Transactional
    public BookmarkResult toggleBookmark(Integer articleId) {
        Integer userId = CurrentUser.requireId();
        Article article = articleId == null || articleId < 1 ? null : articleMapper.findBasicById(articleId);
        if (article == null || (article.isDraft() && !article.getAuthorId().equals(userId))) {
            throw BusinessException.notFound("文章不存在");
        }
        if (article.isDraft()) {
            throw new BusinessException("草稿发布后才能收藏");
        }

        boolean bookmarked = articleBookmarkMapper.checkIsBookmarked(userId, articleId) == 0;
        if (bookmarked) {
            articleBookmarkMapper.addBookmark(userId, articleId);
        } else {
            articleBookmarkMapper.deleteBookmark(userId, articleId);
        }
        // 收藏是私人行为，不通知作者
        articleMapper.refreshBookmarkCount(articleId);
        int bookmarkCount = articleMapper.selectBookmarkCount(articleId);
        return new BookmarkResult(bookmarked ? "收藏成功" : "已取消收藏", bookmarked, bookmarkCount);
    }

    @Override
    public PageResult<Article> pageMine(Integer page, Integer pageSize) {
        Integer userId = CurrentUser.requireId();
        int normalizedPage = page == null ? 1 : page;
        int normalizedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalizedPage < 1) {
            throw new BusinessException("页码必须大于等于 1");
        }
        if (normalizedPageSize < 1 || normalizedPageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量必须在 1 到 50 之间");
        }
        long total = articleMapper.countBookmarked(userId);
        List<Article> items = total == 0
                ? List.of()
                : articleMapper.pageBookmarked(userId, (normalizedPage - 1) * normalizedPageSize, normalizedPageSize);
        return PageResult.of(items, total, normalizedPage, normalizedPageSize);
    }
}
