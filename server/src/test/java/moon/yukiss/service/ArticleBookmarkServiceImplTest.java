package moon.yukiss.service;

import moon.yukiss.common.BookmarkResult;
import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleBookmarkMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.impl.ArticleBookmarkServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleBookmarkServiceImplTest {
    private ArticleBookmarkMapper bookmarkMapper;
    private ArticleMapper articleMapper;
    private ArticleBookmarkServiceImpl bookmarkService;

    @BeforeEach
    void setUp() {
        bookmarkMapper = mock(ArticleBookmarkMapper.class);
        articleMapper = mock(ArticleMapper.class);
        bookmarkService = new ArticleBookmarkServiceImpl(bookmarkMapper, articleMapper);
        ThreadLocalUtil.set(Map.of("id", 7));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void bookmarkRefreshesStoredCount() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 3, Article.STATUS_PUBLISHED));
        when(bookmarkMapper.checkIsBookmarked(7, 12)).thenReturn(0);
        when(bookmarkMapper.addBookmark(7, 12)).thenReturn(1);
        when(articleMapper.selectBookmarkCount(12)).thenReturn(4);

        BookmarkResult result = bookmarkService.toggleBookmark(12);

        assertTrue(result.bookmarked());
        assertEquals(4, result.bookmarkCount());
        verify(articleMapper).refreshBookmarkCount(12);
    }

    @Test
    void secondToggleRemovesBookmark() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 3, Article.STATUS_PUBLISHED));
        when(bookmarkMapper.checkIsBookmarked(7, 12)).thenReturn(1);
        when(bookmarkMapper.deleteBookmark(7, 12)).thenReturn(1);
        when(articleMapper.selectBookmarkCount(12)).thenReturn(0);

        BookmarkResult result = bookmarkService.toggleBookmark(12);

        assertFalse(result.bookmarked());
        verify(bookmarkMapper, never()).addBookmark(anyInt(), anyInt());
        verify(articleMapper).refreshBookmarkCount(12);
    }

    @Test
    void draftCannotBeBookmarkedEvenByAuthor() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 7, Article.STATUS_DRAFT));

        BusinessException exception = assertThrows(BusinessException.class, () -> bookmarkService.toggleBookmark(12));

        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void otherUsersDraftLooksMissing() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 3, Article.STATUS_DRAFT));

        BusinessException exception = assertThrows(BusinessException.class, () -> bookmarkService.toggleBookmark(12));

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void pageMineSkipsQueryWhenNothingBookmarked() {
        when(articleMapper.countBookmarked(7)).thenReturn(0L);

        PageResult<Article> result = bookmarkService.pageMine(1, 10);

        assertEquals(0, result.getTotal());
        verify(articleMapper, never()).pageBookmarked(anyInt(), anyInt(), anyInt());
    }

    @Test
    void pageMineUsesOffsetFromPage() {
        Article article = article(12, 3, Article.STATUS_PUBLISHED);
        when(articleMapper.countBookmarked(7)).thenReturn(11L);
        when(articleMapper.pageBookmarked(7, 10, 10)).thenReturn(List.of(article));

        PageResult<Article> result = bookmarkService.pageMine(2, 10);

        assertEquals(List.of(article), result.getItems());
        assertEquals(2, result.getTotalPages());
    }

    private Article article(int id, int authorId, String status) {
        Article article = new Article();
        article.setId(id);
        article.setAuthorId(authorId);
        article.setStatus(status);
        return article;
    }
}
