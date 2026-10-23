package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.entity.Article;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.ArticleCommentMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.impl.ArticleCommentServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleCommentServiceImplTest {
    private ArticleCommentMapper commentMapper;
    private ArticleMapper articleMapper;
    private NotificationService notificationService;
    private ArticleCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentMapper = mock(ArticleCommentMapper.class);
        articleMapper = mock(ArticleMapper.class);
        notificationService = mock(NotificationService.class);
        commentService = new ArticleCommentServiceImpl(commentMapper, articleMapper, notificationService);
        ThreadLocalUtil.set(Map.of("id", 7));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void addCommentRejectsParentFromAnotherArticle() {
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_PUBLISHED));
        when(commentMapper.findById(9, 7)).thenReturn(comment(9, 2, null, 5));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.addComment(request(1, 9, "回复"))
        );

        assertEquals(400, exception.getStatus().value());
        verify(commentMapper, never()).insert(any());
    }

    @Test
    void addCommentRejectsReplyToReply() {
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_PUBLISHED));
        when(commentMapper.findById(9, 7)).thenReturn(comment(9, 1, 3, 5));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.addComment(request(1, 9, "嵌套回复"))
        );

        assertEquals("目前仅支持回复一级评论", exception.getMessage());
    }

    @Test
    void addCommentRejectsDraftArticle() {
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 7, Article.STATUS_DRAFT));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.addComment(request(1, null, "评论"))
        );

        assertEquals("草稿发布后才能评论", exception.getMessage());
    }

    @Test
    void otherUsersDraftLooksLikeMissingArticle() {
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_DRAFT));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.pageByArticleId(1, 1, 10)
        );

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void replyNotifiesParentAuthorAndArticleAuthorAndRefreshesCounts() {
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_PUBLISHED));
        when(commentMapper.findById(9, 7)).thenReturn(comment(9, 1, null, 5));
        ArticleComment created = comment(12, 1, 9, 7);
        when(commentMapper.findById(12, 7)).thenReturn(created);
        givenInsertAssignsId(12);

        ArticleComment result = commentService.addComment(request(1, 9, "  回复  "));

        assertEquals(created, result);
        verify(articleMapper).refreshCommentStats(1);
        verify(notificationService).notify(5, 7, Notification.TYPE_REPLY, 1, 12);
        verify(notificationService).notify(3, 7, Notification.TYPE_COMMENT, 1, 12);
    }

    @Test
    void replyToArticleAuthorDoesNotNotifyThemTwice() {
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_PUBLISHED));
        when(commentMapper.findById(9, 7)).thenReturn(comment(9, 1, null, 3));
        givenInsertAssignsId(12);

        commentService.addComment(request(1, 9, "回复作者"));

        verify(notificationService).notify(3, 7, Notification.TYPE_REPLY, 1, 12);
        verify(notificationService, never()).notify(3, 7, Notification.TYPE_COMMENT, 1, 12);
    }

    @Test
    void pageAttachesRepliesToTheirParents() {
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_PUBLISHED));
        ArticleComment first = comment(10, 1, null, 5);
        ArticleComment second = comment(11, 1, null, 6);
        ArticleComment reply = comment(20, 1, 10, 7);
        when(commentMapper.countTopLevel(1)).thenReturn(12L);
        when(commentMapper.pageTopLevel(1, 7, 10, 10)).thenReturn(List.of(first, second));
        when(commentMapper.findRepliesByParentIds(anyList(), any())).thenReturn(List.of(reply));

        PageResult<ArticleComment> result = commentService.pageByArticleId(1, 2, 10);

        assertEquals(12, result.getTotal());
        assertEquals(2, result.getTotalPages());
        assertEquals(List.of(reply), first.getReplies());
        assertTrue(second.getReplies().isEmpty());
    }

    @Test
    void onlyCommentAuthorCanEdit() {
        when(commentMapper.findById(12, 7)).thenReturn(comment(12, 1, null, 8));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commentService.updateComment(12, "改一下")
        );

        assertEquals(403, exception.getStatus().value());
        verify(commentMapper, never()).updateContent(anyInt(), any());
    }

    @Test
    void commentAuthorCanEditOwnComment() {
        when(commentMapper.findById(12, 7)).thenReturn(comment(12, 1, null, 7));

        commentService.updateComment(12, "  新内容  ");

        verify(commentMapper).updateContent(12, "新内容");
    }

    @Test
    void strangerCannotDeleteComment() {
        when(commentMapper.findById(12, 7)).thenReturn(comment(12, 1, null, 8));
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 9, Article.STATUS_PUBLISHED));

        BusinessException exception = assertThrows(BusinessException.class, () -> commentService.deleteComment(12));

        assertEquals(403, exception.getStatus().value());
        verify(commentMapper, never()).deleteByIds(anyList());
    }

    @Test
    void articleAuthorCanDeleteTopLevelCommentTogetherWithReplies() {
        when(commentMapper.findById(12, 7)).thenReturn(comment(12, 1, null, 8));
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 7, Article.STATUS_PUBLISHED));
        when(commentMapper.findReplyIds(12)).thenReturn(List.of(30, 31));

        int deleted = commentService.deleteComment(12);

        assertEquals(3, deleted);
        List<Integer> ids = List.of(12, 30, 31);
        verify(commentMapper).deleteLikesByCommentIds(ids);
        verify(notificationService).deleteByCommentIds(ids);
        verify(commentMapper).deleteByIds(ids);
        verify(articleMapper).refreshCommentStats(1);
    }

    @Test
    void deletingReplyOnlyRemovesThatReply() {
        when(commentMapper.findById(30, 7)).thenReturn(comment(30, 1, 12, 7));
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 9, Article.STATUS_PUBLISHED));

        int deleted = commentService.deleteComment(30);

        assertEquals(1, deleted);
        verify(commentMapper, never()).findReplyIds(anyInt());
        verify(commentMapper).deleteByIds(List.of(30));
    }

    @Test
    void toggleLikeNotifiesCommentAuthorAndReturnsFreshCount() {
        when(commentMapper.findById(12, 7)).thenReturn(comment(12, 1, null, 5));
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_PUBLISHED));
        when(commentMapper.checkCommentLiked(7, 12)).thenReturn(0);
        when(commentMapper.addCommentLike(7, 12)).thenReturn(1);
        when(commentMapper.countCommentLikes(12)).thenReturn(5);

        LikeResult result = commentService.toggleLike(12);

        assertTrue(result.liked());
        assertEquals(5, result.likeCount());
        verify(notificationService).notify(5, 7, Notification.TYPE_COMMENT_LIKE, 1, 12);
    }

    @Test
    void unlikeRetractsNotification() {
        when(commentMapper.findById(12, 7)).thenReturn(comment(12, 1, null, 5));
        when(articleMapper.findBasicById(1)).thenReturn(article(1, 3, Article.STATUS_PUBLISHED));
        when(commentMapper.checkCommentLiked(7, 12)).thenReturn(1);
        when(commentMapper.deleteCommentLike(7, 12)).thenReturn(1);

        LikeResult result = commentService.toggleLike(12);

        assertEquals(false, result.liked());
        verify(notificationService).retract(5, 7, Notification.TYPE_COMMENT_LIKE, 1, 12);
    }

    private void givenInsertAssignsId(int id) {
        doAnswer(invocation -> {
            ArticleComment value = invocation.getArgument(0);
            value.setId(id);
            return null;
        }).when(commentMapper).insert(any(ArticleComment.class));
    }

    private CommentRequest request(int articleId, Integer parentId, String content) {
        CommentRequest request = new CommentRequest();
        request.setArticleId(articleId);
        request.setParentId(parentId);
        request.setContent(content);
        return request;
    }

    private ArticleComment comment(int id, int articleId, Integer parentId, int userId) {
        ArticleComment comment = new ArticleComment();
        comment.setId(id);
        comment.setArticleId(articleId);
        comment.setParentId(parentId);
        comment.setUserId(userId);
        return comment;
    }

    private Article article(int id, int authorId, String status) {
        Article article = new Article();
        article.setId(id);
        article.setAuthorId(authorId);
        article.setStatus(status);
        return article;
    }
}
