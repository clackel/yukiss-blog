package moon.yukiss.config;

import moon.yukiss.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration // 告诉 Spring 这是一个配置类
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册保安
        registry.addInterceptor(loginInterceptor)
                // 排除登录、注册、以及获取文章列表的接口 (看别人文章不用登录)
                .excludePathPatterns("/user/login",
                        "/user/register",
                        "/user/recover/code",
                        "/user/recover/account",
                        "/uploads/**",
                        "/",             // 放行默认主页
                        "/index.html",   // 放行主页文件
                        "/assets/**",    // 放行 Vue 打包出来的 css 和 js
                        "/*.ico",        // 放行网站图标
                        "/*.svg",        // 放行 svg 图标
                        "/*.png"         // 放行根目录的图片)
                )
                .addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        File dir = path.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(path.toUri().toString());
    }
}
