package moon.yukiss.service;

import moon.yukiss.mapper.ArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ArticleViewServiceTest {
    private ArticleMapper articleMapper;
    private ArticleViewService viewService;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        viewService = new ArticleViewService(articleMapper);
    }

    @Test
    void viewsAreBufferedUntilFlush() {
        viewService.record(12);
        viewService.record(12);
        viewService.record(30);

        assertEquals(2, viewService.pendingCount(12));
        verify(articleMapper, never()).addViewCount(anyInt(), anyLong());

        viewService.flush();

        verify(articleMapper).addViewCount(12, 2);
        verify(articleMapper).addViewCount(30, 1);
        assertEquals(0, viewService.pendingCount(12));
    }

    @Test
    void flushWithoutNewViewsWritesNothing() {
        viewService.record(12);
        viewService.flush();
        viewService.flush();

        verify(articleMapper, times(1)).addViewCount(12, 1);
    }

    @Test
    void failedWriteKeepsCountForNextFlush() {
        doThrow(new RuntimeException("db down")).when(articleMapper).addViewCount(12, 3);
        viewService.record(12);
        viewService.record(12);
        viewService.record(12);

        viewService.flush();

        assertEquals(3, viewService.pendingCount(12));
    }
}
