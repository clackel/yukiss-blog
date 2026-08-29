package moon.yukiss.controller;


import moon.yukiss.common.ApiResponse;
import moon.yukiss.service.ArticleLikeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/like")
public class ArticleLikeController {
    private final ArticleLikeService articleLikeService;

    public ArticleLikeController(ArticleLikeService articleLikeService) {
        this.articleLikeService = articleLikeService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> toggle(Integer articleId){
        String message = articleLikeService.toggleLike(articleId);
        return ApiResponse.ok(Map.of(
                "message", message,
                "liked", "点赞成功".equals(message),
                "likeCount", articleLikeService.countLikes(articleId)
        ));
    }
}
