package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.mapper.ArticleCommentMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleCommentService;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ArticleCommentServiceImpl implements ArticleCommentService {
    private final ArticleCommentMapper articleCommentMapper;
    private final ArticleMapper articleMapper;

    public ArticleCommentServiceImpl(
            ArticleCommentMapper articleCommentMapper,
            ArticleMapper articleMapper
    ) {
        this.articleCommentMapper = articleCommentMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public ArticleComment addComment(CommentRequest request) {
        Integer userId = requireCurrentUserId();
        requireArticle(request.getArticleId());

        if (request.getParentId() != null) {
            ArticleComment parent = articleCommentMapper.findById(request.getParentId(), userId);
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
        return articleCommentMapper.findById(comment.getId(), userId);
    }

    @Override
    public List<ArticleComment> listByArticleId(Integer articleId) {
        requireArticle(articleId);
        return articleCommentMapper.findByArticleId(articleId, currentUserIdOrNull());
    }

    @Override
    public LikeResult toggleLike(Integer commentId) {
        Integer userId = requireCurrentUserId();
        ArticleComment comment = articleCommentMapper.findById(commentId, userId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }

        boolean liked = articleCommentMapper.checkCommentLiked(userId, commentId) == 0;
        if (liked) {
            articleCommentMapper.addCommentLike(userId, commentId);
        } else {
            articleCommentMapper.deleteCommentLike(userId, commentId);
        }
        int likeCount = articleCommentMapper.countCommentLikes(commentId);
        return new LikeResult(liked ? "点赞成功" : "取消点赞", liked, likeCount);
    }

    private void requireArticle(Integer articleId) {
        if (articleId == null || articleId < 1 || articleMapper.existsById(articleId) == 0) {
            throw BusinessException.notFound("文章不存在");
        }
    }

    private Integer currentUserIdOrNull() {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        if (userMap == null) {
            return null;
        }
        Object id = userMap.get("id");
        return id instanceof Number ? ((Number) id).intValue() : null;
    }

    private Integer requireCurrentUserId() {
        Integer userId = currentUserIdOrNull();
        if (userId == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return userId;
    }
}
