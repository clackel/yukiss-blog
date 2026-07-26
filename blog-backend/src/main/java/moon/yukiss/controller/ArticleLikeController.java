package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.LikeResult;
import moon.yukiss.service.ArticleLikeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
public class ArticleLikeController {
    private final ArticleLikeService articleLikeService;

    public ArticleLikeController(ArticleLikeService articleLikeService) {
        this.articleLikeService = articleLikeService;
    }

    @PostMapping
    public ApiResponse<LikeResult> toggle(@RequestParam Integer articleId) {
        return ApiResponse.ok(articleLikeService.toggleLike(articleId));
    }
}
