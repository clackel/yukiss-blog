package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.mapper.ArticleLikeMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.impl.ArticleLikeServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleLikeServiceImplTest {
    private ArticleLikeMapper likeMapper;
    private ArticleMapper articleMapper;
    private ArticleLikeServiceImpl likeService;

    @BeforeEach
    void setUp() {
        likeMapper = mock(ArticleLikeMapper.class);
        articleMapper = mock(ArticleMapper.class);
        likeService = new ArticleLikeServiceImpl(likeMapper, articleMapper);
        ThreadLocalUtil.set(Map.of("id", 7));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void togglesLikeAndReturnsFreshCount() {
        when(articleMapper.existsById(12)).thenReturn(1);
        when(likeMapper.checkIsLiked(7, 12)).thenReturn(0);
        when(likeMapper.countLikes(12)).thenReturn(9);

        LikeResult result = likeService.toggleLike(12);

        assertTrue(result.liked());
        assertEquals(9, result.likeCount());
        verify(likeMapper).addLike(7, 12);
    }

    @Test
    void rejectsMissingArticle() {
        when(articleMapper.existsById(99)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> likeService.toggleLike(99)
        );

        assertEquals(404, exception.getStatus().value());
    }
}
