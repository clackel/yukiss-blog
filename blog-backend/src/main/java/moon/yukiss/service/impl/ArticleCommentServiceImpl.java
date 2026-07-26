package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.mapper.ArticleCommentMapper;
import moon.yukiss.service.ArticleCommentService;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ArticleCommentServiceImpl implements ArticleCommentService {
    @Autowired
    private ArticleCommentMapper articleCommentMapper;

    // ServiceImpl 核心方法
    @Override
    public void addComment(ArticleComment comment) {
        // 自动补齐当前登录者 ID
        comment.setUserId(requireCurrentUserId());

        articleCommentMapper.insert(comment);
    }

    @Override
    public List<ArticleComment> listByArticleId(Integer articleId) {
        return articleCommentMapper.findByArticleId(articleId, currentUserIdOrNull());
    }

    @Override
    public String toggleLike(Integer commentId) {
        Integer userId = requireCurrentUserId();
        int count = articleCommentMapper.checkCommentLiked(userId, commentId);
        if (count > 0) {
            articleCommentMapper.deleteCommentLike(userId, commentId);
            return "取消点赞";
        }
        articleCommentMapper.addCommentLike(userId, commentId);
        return "点赞成功";
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
            throw new BusinessException("请先登录");
        }
        return userId;
    }
}
