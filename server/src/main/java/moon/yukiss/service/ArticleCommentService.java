package moon.yukiss.service;

import moon.yukiss.common.LikeResult;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.entity.ArticleComment;

public interface ArticleCommentService {
    ArticleComment addComment(CommentRequest request);

    /** 一级评论分页，每条一级评论携带其全部回复 */
    PageResult<ArticleComment> pageByArticleId(Integer articleId, Integer page, Integer pageSize);

    ArticleComment updateComment(Integer commentId, String content);

    /** 删除评论（一级评论连同回复一起删除），返回删除的评论条数 */
    int deleteComment(Integer commentId);

    LikeResult toggleLike(Integer commentId);
}
