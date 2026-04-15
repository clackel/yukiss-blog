package moon.yukiss.config;

import moon.yukiss.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration // 告诉 Spring 这是一个配置类
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册保安
        registry.addInterceptor(loginInterceptor)
                // 排除登录、注册、以及获取文章列表的接口 (看别人文章不用登录)
                .excludePathPatterns("/user/login",
                        "/user/register",
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
        // 获取当前项目运行的绝对路径，并在里面建一个 uploads 文件夹
        String path = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs(); // 如果文件夹不存在，就自动创建
        }
        // 告诉 Spring：当网址请求 /uploads/** 时，映射到本地的 uploads 文件夹
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + path);
    }
}