package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.mapper.ArticleLikeMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleLikeService;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleMapper articleMapper;

    public ArticleLikeServiceImpl(
            ArticleLikeMapper articleLikeMapper,
            ArticleMapper articleMapper
    ) {
        this.articleLikeMapper = articleLikeMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public LikeResult toggleLike(Integer articleId) {
        Integer userId = requireCurrentUserId();
        if (articleId == null || articleId < 1 || articleMapper.existsById(articleId) == 0) {
            throw BusinessException.notFound("文章不存在");
        }

        boolean liked = articleLikeMapper.checkIsLiked(userId, articleId) == 0;
        if (liked) {
            articleLikeMapper.addLike(userId, articleId);
        } else {
            articleLikeMapper.deleteLike(userId, articleId);
        }
        int likeCount = articleLikeMapper.countLikes(articleId);
        return new LikeResult(liked ? "点赞成功" : "取消点赞", liked, likeCount);
    }

    private Integer requireCurrentUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Object id = map == null ? null : map.get("id");
        if (!(id instanceof Number)) {
            throw BusinessException.unauthorized("请先登录");
        }
        return ((Number) id).intValue();
    }
}
