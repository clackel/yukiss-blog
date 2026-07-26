package moon.yukiss.service;

import moon.yukiss.common.LikeResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.entity.ArticleComment;

import java.util.List;

public interface ArticleCommentService {
    ArticleComment addComment(CommentRequest request);

    List<ArticleComment> listByArticleId(Integer articleId);

    LikeResult toggleLike(Integer commentId);
}
