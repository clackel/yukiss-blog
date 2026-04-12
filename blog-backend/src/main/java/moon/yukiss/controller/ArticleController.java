package moon.yukiss.controller;

import moon.yukiss.entity.Article;
import moon.yukiss.service.ArticleService; // 1. 改为引入 Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService; // 2. 类型修改为 ArticleService

    // 查全部
    @GetMapping
    public List<Article> list() {
        // 现在调用的是 Service 层的 list 方法
        return articleService.list();
    }

    // 查单篇：{id} 是占位符
    @GetMapping("/{id}")
    public Article getById(@PathVariable Integer id) {
        // 现在调用的是 Service 层的 getById 方法
        return articleService.getById(id);
    }

    // 发布新文章
    @PostMapping
    public String add(@RequestBody Article article) {
        // 核心修正：调用 Service 层的 add 方法，它会自动映射到 Mapper 的 insert
        articleService.add(article);
        return "发布成功！";
    }
}