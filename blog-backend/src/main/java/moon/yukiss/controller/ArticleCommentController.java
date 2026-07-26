package moon.yukiss.controller;

import jakarta.validation.Valid;
import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.LikeResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.service.ArticleCommentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class ArticleCommentController {
    private final ArticleCommentService commentService;

    public ArticleCommentController(ArticleCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/add")
    public ApiResponse<ArticleComment> add(@Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(commentService.addComment(request));
    }

    @GetMapping("/list")
    public ApiResponse<List<ArticleComment>> list(@RequestParam Integer articleId) {
        return ApiResponse.ok(commentService.listByArticleId(articleId));
    }

    @PostMapping("/{commentId}/like")
    public ApiResponse<LikeResult> toggleLike(@PathVariable Integer commentId) {
        return ApiResponse.ok(commentService.toggleLike(commentId));
    }
}
