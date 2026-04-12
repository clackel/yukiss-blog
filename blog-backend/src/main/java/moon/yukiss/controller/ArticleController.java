package moon.yukiss.controller;

import moon.yukiss.entity.Article;
import moon.yukiss.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public List<Article> list() {
        return articleService.list();
    }

    @GetMapping("/{id}")
    public Article getById(@PathVariable Integer id) {
        return articleService.getById(id);
    }

    @PostMapping
    public String add(@RequestBody Article article) {
        articleService.add(article);
        return "发布成功！";
    }
}