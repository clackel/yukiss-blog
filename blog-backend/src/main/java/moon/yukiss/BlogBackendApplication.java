package moon.yukiss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogBackendApplication {
    public static void main(String[] args) {
        // 这一行代码，唤醒整个 Spring Boot 生态
        SpringApplication.run(BlogBackendApplication.class, args);
        System.out.println("====== 博客后端启动成功！ ======");
    }
}