package moon.yukiss.service;

import moon.yukiss.common.LikeResult;

public interface ArticleLikeService {
    LikeResult toggleLike(Integer articleId);
}
