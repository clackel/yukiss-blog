package moon.yukiss.controller;

import moon.yukiss.entity.ArticleComment;
import moon.yukiss.mapper.ArticleCommentMapper;
import moon.yukiss.service.ArticleCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class ArticleCommentController {
    @Autowired
    private ArticleCommentService CommentService;

    // 发布评论
    @PostMapping("/add")
    public String add(@RequestBody ArticleComment comment){
        CommentService.addComment(comment);
        return "评论成功~";
    }

    // 获取文章评论列表
    @GetMapping("/list")
    public List<ArticleComment> list(Integer articleId){
        return CommentService.listByArticleId(articleId);
    }
}
