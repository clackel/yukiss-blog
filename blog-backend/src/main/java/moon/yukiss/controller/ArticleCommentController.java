package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.service.ArticleCommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class ArticleCommentController {
    private final ArticleCommentService commentService;

    public ArticleCommentController(ArticleCommentService commentService) {
        this.commentService = commentService;
    }

    // 发布评论
    @PostMapping("/add")
    public ApiResponse<Void> add(@RequestBody ArticleComment comment){
        commentService.addComment(comment);
        return ApiResponse.ok("评论成功");
    }

    // 获取文章评论列表
    @GetMapping("/list")
    public ApiResponse<List<ArticleComment>> list(Integer articleId){
        return ApiResponse.ok(commentService.listByArticleId(articleId));
    }

    @PostMapping("/{commentId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Integer commentId) {
        String message = commentService.toggleLike(commentId);
        return ApiResponse.ok(Map.of(
                "message", message,
                "liked", "点赞成功".equals(message)
        ));
    }
}
