package moon.yukiss.service;

import moon.yukiss.entity.ArticleComment;
import moon.yukiss.mapper.ArticleCommentMapper;
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
        Map<String, Object> userMap = ThreadLocalUtil.get();
        comment.setUserId((Integer) userMap.get("id"));

        articleCommentMapper.insert(comment);
    }

    @Override
    public List<ArticleComment> listByArticleId(Integer articleId) {
        return articleCommentMapper.findByArticleId(articleId, currentUserId());
    }

    @Override
    public String toggleLike(Integer commentId) {
        Integer userId = currentUserId();
        int count = articleCommentMapper.checkCommentLiked(userId, commentId);
        if (count > 0) {
            articleCommentMapper.deleteCommentLike(userId, commentId);
            return "取消点赞";
        }
        articleCommentMapper.addCommentLike(userId, commentId);
        return "点赞成功";
    }

    private Integer currentUserId() {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        return (Integer) userMap.get("id");
    }
}
