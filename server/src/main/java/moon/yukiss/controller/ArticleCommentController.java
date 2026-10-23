package moon.yukiss.controller;

import jakarta.validation.Valid;
import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.LikeResult;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.CommentRequest;
import moon.yukiss.dto.CommentUpdateRequest;
import moon.yukiss.entity.ArticleComment;
import moon.yukiss.service.ArticleCommentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public ApiResponse<PageResult<ArticleComment>> list(
            @RequestParam Integer articleId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.ok(commentService.pageByArticleId(articleId, page, pageSize));
    }

    @PutMapping("/{commentId}")
    public ApiResponse<ArticleComment> update(
            @PathVariable Integer commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ApiResponse.ok(commentService.updateComment(commentId, request.getContent()));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Map<String, Integer>> delete(@PathVariable Integer commentId) {
        int deletedCount = commentService.deleteComment(commentId);
        return ApiResponse.ok("评论已删除", Map.of("deletedCount", deletedCount));
    }

    @PostMapping("/{commentId}/like")
    public ApiResponse<LikeResult> toggleLike(@PathVariable Integer commentId) {
        return ApiResponse.ok(commentService.toggleLike(commentId));
    }
}
