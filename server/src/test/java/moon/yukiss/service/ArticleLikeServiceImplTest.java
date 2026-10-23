package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.entity.Article;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.ArticleLikeMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.impl.ArticleLikeServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleLikeServiceImplTest {
    private ArticleLikeMapper likeMapper;
    private ArticleMapper articleMapper;
    private NotificationService notificationService;
    private ArticleLikeServiceImpl likeService;

    @BeforeEach
    void setUp() {
        likeMapper = mock(ArticleLikeMapper.class);
        articleMapper = mock(ArticleMapper.class);
        notificationService = mock(NotificationService.class);
        likeService = new ArticleLikeServiceImpl(likeMapper, articleMapper, notificationService);
        ThreadLocalUtil.set(Map.of("id", 7));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void likeRefreshesStoredCountAndNotifiesAuthor() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 3, Article.STATUS_PUBLISHED));
        when(likeMapper.checkIsLiked(7, 12)).thenReturn(0);
        when(likeMapper.addLike(7, 12)).thenReturn(1);
        when(articleMapper.selectLikeCount(12)).thenReturn(9);

        LikeResult result = likeService.toggleLike(12);

        assertTrue(result.liked());
        assertEquals(9, result.likeCount());
        verify(articleMapper).refreshLikeCount(12);
        verify(notificationService).notify(3, 7, Notification.TYPE_ARTICLE_LIKE, 12, null);
    }

    @Test
    void duplicateConcurrentLikeDoesNotNotifyTwice() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 3, Article.STATUS_PUBLISHED));
        when(likeMapper.checkIsLiked(7, 12)).thenReturn(0);
        when(likeMapper.addLike(7, 12)).thenReturn(0);

        likeService.toggleLike(12);

        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    void unlikeRetractsNotification() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 3, Article.STATUS_PUBLISHED));
        when(likeMapper.checkIsLiked(7, 12)).thenReturn(1);
        when(likeMapper.deleteLike(7, 12)).thenReturn(1);

        LikeResult result = likeService.toggleLike(12);

        assertFalse(result.liked());
        verify(notificationService).retract(3, 7, Notification.TYPE_ARTICLE_LIKE, 12, null);
    }

    @Test
    void rejectsMissingArticle() {
        when(articleMapper.findBasicById(99)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> likeService.toggleLike(99));

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void rejectsLikingDraft() {
        when(articleMapper.findBasicById(12)).thenReturn(article(12, 7, Article.STATUS_DRAFT));

        BusinessException exception = assertThrows(BusinessException.class, () -> likeService.toggleLike(12));

        assertEquals(400, exception.getStatus().value());
        verify(likeMapper, never()).addLike(anyInt(), anyInt());
    }

    private Article article(int id, int authorId, String status) {
        Article article = new Article();
        article.setId(id);
        article.setAuthorId(authorId);
        article.setStatus(status);
        return article;
    }
}
