package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.ArticleRequest;
import moon.yukiss.entity.Article;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.impl.ArticleServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceImplTest {
    private ArticleMapper articleMapper;
    private ArticleServiceImpl articleService;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        articleService = new ArticleServiceImpl(articleMapper);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void pageNormalizesSearchAndPopularSort() {
        ThreadLocalUtil.set(Map.of("id", 7));
        Article article = article(11, 3);
        when(articleMapper.countPage("Spring")).thenReturn(12L);
        when(articleMapper.page(7, "Spring", "popular", 10, 10)).thenReturn(List.of(article));

        PageResult<Article> result = articleService.page(2, 10, "  Spring  ", "POPULAR");

        assertEquals(12, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(2, result.getTotalPages());
        assertEquals(List.of(article), result.getItems());
    }

    @Test
    void pageRejectsUnsupportedSort() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> articleService.page(1, 10, null, "oldest")
        );

        assertEquals(400, exception.getStatus().value());
        verify(articleMapper, never()).countPage(any());
    }

    @Test
    void addTrimsContentAndReturnsCreatedArticle() {
        ThreadLocalUtil.set(Map.of("id", 7));
        ArticleRequest request = articleRequest("  标题  ", "  正文  ");
        Article created = article(21, 7);
        created.setTitle("标题");
        created.setContent("正文");
        doAnswer(invocation -> {
            Article value = invocation.getArgument(0);
            value.setId(21);
            return 1;
        }).when(articleMapper).insert(any(Article.class));
        when(articleMapper.getById(21, 7)).thenReturn(created);

        Article result = articleService.add(request);

        assertEquals(created, result);
        verify(articleMapper).insert(any(Article.class));
    }

    @Test
    void updateRejectsAnotherUsersArticle() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(articleMapper.getById(21, 7)).thenReturn(article(21, 8));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> articleService.update(21, articleRequest("标题", "正文"))
        );

        assertEquals(403, exception.getStatus().value());
        verify(articleMapper, never()).update(any());
    }

    @Test
    void deleteRemovesDependentInteractionsBeforeArticle() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(articleMapper.getById(21, 7)).thenReturn(article(21, 7));

        articleService.delete(21);

        InOrder order = inOrder(articleMapper);
        order.verify(articleMapper).deleteCommentLikesByArticleId(21);
        order.verify(articleMapper).detachCommentRepliesByArticleId(21);
        order.verify(articleMapper).deleteCommentsByArticleId(21);
        order.verify(articleMapper).deleteLikesByArticleId(21);
        order.verify(articleMapper).deleteById(21);
    }

    private Article article(int id, int authorId) {
        Article article = new Article();
        article.setId(id);
        article.setAuthorId(authorId);
        return article;
    }

    private ArticleRequest articleRequest(String title, String content) {
        ArticleRequest request = new ArticleRequest();
        request.setTitle(title);
        request.setContent(content);
        return request;
    }
}
