package moon.yukiss.service;

import moon.yukiss.entity.ArticleComment;

import java.util.List;

public interface ArticleCommentService {

    void addComment(ArticleComment comment);

    List<ArticleComment> listByArticleId(Integer articleId);
}
