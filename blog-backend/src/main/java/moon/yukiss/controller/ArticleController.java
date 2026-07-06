package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.entity.Article;
import moon.yukiss.service.ArticleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public ApiResponse<List<Article>> list() {
        return ApiResponse.ok(articleService.list());
    }

    @GetMapping("/mine")
    public ApiResponse<List<Article>> mine() {
        return ApiResponse.ok(articleService.listMine());
    }

    @GetMapping("/{id}")
    public ApiResponse<Article> getById(@PathVariable Integer id) {
        return ApiResponse.ok(articleService.getById(id));
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody Article article) {
        articleService.add(article);
        return ApiResponse.ok("发布成功");
    }
}
