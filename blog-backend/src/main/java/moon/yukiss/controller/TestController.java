package moon.yukiss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // 当浏览器访问 /hello 这个地址时，就会执行下面这个方法
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello World! 这是我的第一个全栈博客接口！";
    }
}