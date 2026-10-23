package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.entity.Article;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.ArticleCommentMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleCommentService;
import moon.yukiss.service.NotificationService;
import moon.yukiss.utils.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArticleCommentServiceImpl implements ArticleCommentService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ArticleCommentMapper articleCommentMapper;
    private final ArticleMapper articleMapper;
    private final NotificationService notificationService;

    public ArticleCommentServiceImpl(
            ArticleCommentMapper articleCommentMapper,
            ArticleMapper articleMapper,
            NotificationService notificationService
    ) {
        this.articleCommentMapper = articleCommentMapper;
        this.articleMapper = articleMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ArticleComment addComment(CommentRequest request) {
        Integer userId = CurrentUser.requireId();
        Article article = requirePublishedArticle(request.getArticleId(), userId);

        ArticleComment parent = null;
        if (request.getParentId() != null) {
            parent = articleCommentMapper.findById(request.getParentId(), userId);
            if (parent == null) {
                throw BusinessException.notFound("要回复的评论不存在");
            }
            if (!request.getArticleId().equals(parent.getArticleId())) {
                throw new BusinessException("父评论不属于当前文章");
            }
            if (parent.getParentId() != null) {
                throw new BusinessException("目前仅支持回复一级评论");
            }
        }

        ArticleComment comment = new ArticleComment();
        comment.setArticleId(request.getArticleId());
        comment.setParentId(request.getParentId());
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        articleCommentMapper.insert(comment);
        articleMapper.refreshCommentStats(article.getId());

        // 回复通知被回复的人；文章作者另收一条评论通知（作者就是被回复的人时不重复通知）
        if (parent != null) {
            notificationService.notify(parent.getUserId(), userId, Notification.TYPE_REPLY, article.getId(), comment.getId());
        }
        if (parent == null || !article.getAuthorId().equals(parent.getUserId())) {
            notificationService.notify(article.getAuthorId(), userId, Notification.TYPE_COMMENT, article.getId(), comment.getId());
        }
        return articleCommentMapper.findById(comment.getId(), userId);
    }

    @Override
    public PageResult<ArticleComment> pageByArticleId(Integer articleId, Integer page, Integer pageSize) {
        Integer currentUserId = CurrentUser.idOrNull();
        requireVisibleArticle(articleId, currentUserId);
        int normalizedPage = page == null ? 1 : page;
        int normalizedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalizedPage < 1) {
            throw new BusinessException("页码必须大于等于 1");
        }
        if (normalizedPageSize < 1 || normalizedPageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量必须在 1 到 50 之间");
        }

        long total = articleCommentMapper.countTopLevel(articleId);
        if (total == 0) {
            return PageResult.of(List.of(), 0, normalizedPage, normalizedPageSize);
        }
        List<ArticleComment> topLevel = articleCommentMapper.pageTopLevel(
                articleId,
                currentUserId,
                (normalizedPage - 1) * normalizedPageSize,
                normalizedPageSize
        );
        if (!topLevel.isEmpty()) {
            Map<Integer, ArticleComment> byId = topLevel.stream()
                    .collect(Collectors.toMap(ArticleComment::getId, Function.identity()));
            List<ArticleComment> replies = articleCommentMapper.findRepliesByParentIds(
                    new ArrayList<>(byId.keySet()),
                    currentUserId
            );
            for (ArticleComment reply : replies) {
                ArticleComment parent = byId.get(reply.getParentId());
                if (parent != null) {
                    parent.getReplies().add(reply);
                }
            }
        }
        return PageResult.of(topLevel, total, normalizedPage, normalizedPageSize);
    }

    @Override
    public ArticleComment updateComment(Integer commentId, String content) {
        Integer userId = CurrentUser.requireId();
        ArticleComment comment = requireComment(commentId, userId);
        if (!userId.equals(comment.getUserId())) {
            throw BusinessException.forbidden("只能编辑自己的评论");
        }
        articleCommentMapper.updateContent(commentId, content.trim());
        return articleCommentMapper.findById(commentId, userId);
    }

    @Override
    @Transactional
    public int deleteComment(Integer commentId) {
        Integer userId = CurrentUser.requireId();
        ArticleComment comment = requireComment(commentId, userId);
        Article article = articleMapper.findBasicById(comment.getArticleId());
        boolean isCommentAuthor = userId.equals(comment.getUserId());
        boolean isArticleAuthor = article != null && userId.equals(article.getAuthorId());
        if (!isCommentAuthor && !isArticleAuthor) {
            throw BusinessException.forbidden("只能删除自己的评论或自己文章下的评论");
        }

        List<Integer> ids = new ArrayList<>();
        ids.add(commentId);
        if (comment.getParentId() == null) {
            ids.addAll(articleCommentMapper.findReplyIds(commentId));
        }
        articleCommentMapper.deleteLikesByCommentIds(ids);
        notificationService.deleteByCommentIds(ids);
        articleCommentMapper.deleteByIds(ids);
        articleMapper.refreshCommentStats(comment.getArticleId());
        return ids.size();
    }

    @Override
    @Transactional
    public LikeResult toggleLike(Integer commentId) {
        Integer userId = CurrentUser.requireId();
        ArticleComment comment = requireComment(commentId, userId);
        requireVisibleArticle(comment.getArticleId(), userId);

        boolean liked = articleCommentMapper.checkCommentLiked(userId, commentId) == 0;
        if (liked) {
            if (articleCommentMapper.addCommentLike(userId, commentId) > 0) {
                notificationService.notify(comment.getUserId(), userId, Notification.TYPE_COMMENT_LIKE,
                        comment.getArticleId(), commentId);
            }
        } else if (articleCommentMapper.deleteCommentLike(userId, commentId) > 0) {
            notificationService.retract(comment.getUserId(), userId, Notification.TYPE_COMMENT_LIKE,
                    comment.getArticleId(), commentId);
        }
        int likeCount = articleCommentMapper.countCommentLikes(commentId);
        return new LikeResult(liked ? "点赞成功" : "取消点赞", liked, likeCount);
    }

    private ArticleComment requireComment(Integer commentId, Integer userId) {
        ArticleComment comment = commentId == null ? null : articleCommentMapper.findById(commentId, userId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }
        return comment;
    }

    /** 文章存在且当前用户可见（已发布，或是作者本人的草稿） */
    private Article requireVisibleArticle(Integer articleId, Integer currentUserId) {
        Article article = articleId == null || articleId < 1 ? null : articleMapper.findBasicById(articleId);
        if (article == null || (article.isDraft() && !article.getAuthorId().equals(currentUserId))) {
            throw BusinessException.notFound("文章不存在");
        }
        return article;
    }

    private Article requirePublishedArticle(Integer articleId, Integer currentUserId) {
        Article article = requireVisibleArticle(articleId, currentUserId);
        if (article.isDraft()) {
            throw new BusinessException("草稿发布后才能评论");
        }
        return article;
    }
}
