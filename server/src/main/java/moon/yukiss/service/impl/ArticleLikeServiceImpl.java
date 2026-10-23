package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.LikeResult;
import moon.yukiss.entity.Article;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.ArticleLikeMapper;
import moon.yukiss.mapper.ArticleMapper;
import moon.yukiss.service.ArticleLikeService;
import moon.yukiss.service.NotificationService;
import moon.yukiss.utils.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleMapper articleMapper;
    private final NotificationService notificationService;

    public ArticleLikeServiceImpl(
            ArticleLikeMapper articleLikeMapper,
            ArticleMapper articleMapper,
            NotificationService notificationService
    ) {
        this.articleLikeMapper = articleLikeMapper;
        this.articleMapper = articleMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public LikeResult toggleLike(Integer articleId) {
        Integer userId = CurrentUser.requireId();
        Article article = articleId == null || articleId < 1 ? null : articleMapper.findBasicById(articleId);
        if (article == null || (article.isDraft() && !article.getAuthorId().equals(userId))) {
            throw BusinessException.notFound("文章不存在");
        }
        if (article.isDraft()) {
            throw new BusinessException("草稿发布后才能点赞");
        }

        boolean liked = articleLikeMapper.checkIsLiked(userId, articleId) == 0;
        if (liked) {
            if (articleLikeMapper.addLike(userId, articleId) > 0) {
                notificationService.notify(article.getAuthorId(), userId, Notification.TYPE_ARTICLE_LIKE, articleId, null);
            }
        } else if (articleLikeMapper.deleteLike(userId, articleId) > 0) {
            notificationService.retract(article.getAuthorId(), userId, Notification.TYPE_ARTICLE_LIKE, articleId, null);
        }
        articleMapper.refreshLikeCount(articleId);
        int likeCount = articleMapper.selectLikeCount(articleId);
        return new LikeResult(liked ? "点赞成功" : "取消点赞", liked, likeCount);
    }
}
