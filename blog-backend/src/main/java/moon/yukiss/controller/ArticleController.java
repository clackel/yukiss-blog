package moon.yukiss.controller;

import jakarta.validation.Valid;
import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.PageResult;
import moon.yukiss.dto.ArticleRequest;
import moon.yukiss.entity.Article;
import moon.yukiss.service.ArticleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/page")
    public ApiResponse<PageResult<Article>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return ApiResponse.ok(articleService.page(page, pageSize, keyword, sort));
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
    public ApiResponse<Article> add(@Valid @RequestBody ArticleRequest request) {
        return ApiResponse.ok(articleService.add(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Article> update(
            @PathVariable Integer id,
            @Valid @RequestBody ArticleRequest request
    ) {
        return ApiResponse.ok(articleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        articleService.delete(id);
        return ApiResponse.ok("文章已删除");
    }
}
