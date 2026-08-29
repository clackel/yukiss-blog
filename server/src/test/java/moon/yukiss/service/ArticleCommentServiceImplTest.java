package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.mapper.ArticleCommentMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.impl.ArticleCommentServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleCommentServiceImplTest {
    private ArticleCommentMapper commentMapper;
    private ArticleMapper articleMapper;
    private ArticleCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentMapper = mock(ArticleCommentMapper.class);
        articleMapper = mock(ArticleMapper.class);
        commentService = new ArticleCommentServiceImpl(commentMapper, articleMapper);
        ThreadLocalUtil.set(Map.of("id", 7));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void addCommentRejectsParentFromAnotherArticle() {
        when(articleMapper.existsById(1)).thenReturn(1);
        ArticleComment parent = comment(9, 2, null);
        when(commentMapper.findById(9, 7)).thenReturn(parent);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.addComment(request(1, 9, "回复"))
        );

        assertEquals(400, exception.getStatus().value());
        verify(commentMapper, never()).insert(any());
    }

    @Test
    void addCommentRejectsReplyToReply() {
        when(articleMapper.existsById(1)).thenReturn(1);
        ArticleComment parent = comment(9, 1, 3);
        when(commentMapper.findById(9, 7)).thenReturn(parent);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.addComment(request(1, 9, "嵌套回复"))
        );

        assertEquals("目前仅支持回复一级评论", exception.getMessage());
    }

    @Test
    void addCommentReturnsCreatedReply() {
        when(articleMapper.existsById(1)).thenReturn(1);
        ArticleComment parent = comment(9, 1, null);
        ArticleComment created = comment(12, 1, 9);
        created.setContent("回复");
        when(commentMapper.findById(9, 7)).thenReturn(parent);
        when(commentMapper.findById(12, 7)).thenReturn(created);
        doAnswer(invocation -> {
            ArticleComment value = invocation.getArgument(0);
            value.setId(12);
            return null;
        }).when(commentMapper).insert(any(ArticleComment.class));

        ArticleComment result = commentService.addComment(request(1, 9, "  回复  "));

        assertEquals(created, result);
        verify(commentMapper).insert(any(ArticleComment.class));
    }

    @Test
    void toggleLikeReturnsFreshCount() {
        when(commentMapper.findById(12, 7)).thenReturn(comment(12, 1, null));
        when(commentMapper.checkCommentLiked(7, 12)).thenReturn(0);
        when(commentMapper.countCommentLikes(12)).thenReturn(5);

        LikeResult result = commentService.toggleLike(12);

        assertTrue(result.liked());
        assertEquals(5, result.likeCount());
        verify(commentMapper).addCommentLike(7, 12);
    }

    private CommentRequest request(int articleId, Integer parentId, String content) {
        CommentRequest request = new CommentRequest();
        request.setArticleId(articleId);
        request.setParentId(parentId);
        request.setContent(content);
        return request;
    }

    private ArticleComment comment(int id, int articleId, Integer parentId) {
        ArticleComment comment = new ArticleComment();
        comment.setId(id);
        comment.setArticleId(articleId);
        comment.setParentId(parentId);
        return comment;
    }
}
