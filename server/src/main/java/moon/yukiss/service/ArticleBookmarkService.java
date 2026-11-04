package moon.yukiss.service;

import moon.yukiss.common.BookmarkResult;
import moon.yukiss.common.PageResult;
import moon.yukiss.entity.Article;

public interface ArticleBookmarkService {
    /** 收藏 / 取消收藏切换 */
    BookmarkResult toggleBookmark(Integer articleId);

    /** 当前用户收藏的文章，按收藏时间倒序；已变成草稿的文章不返回 */
    PageResult<Article> pageMine(Integer page, Integer pageSize);
}
