package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.BookmarkResult;
import moon.yukiss.common.PageResult;
import moon.yukiss.entity.Article;
import moon.yukiss.service.ArticleBookmarkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookmark")
public class ArticleBookmarkController {
    private final ArticleBookmarkService articleBookmarkService;

    public ArticleBookmarkController(ArticleBookmarkService articleBookmarkService) {
        this.articleBookmarkService = articleBookmarkService;
    }

    @PostMapping
    public ApiResponse<BookmarkResult> toggle(@RequestParam Integer articleId) {
        return ApiResponse.ok(articleBookmarkService.toggleBookmark(articleId));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<Article>> mine(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.ok(articleBookmarkService.pageMine(page, pageSize));
    }
}
