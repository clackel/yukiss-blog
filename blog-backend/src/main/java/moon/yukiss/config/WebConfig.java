package moon.yukiss.config;

import moon.yukiss.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final String uploadDir;
    private final String[] allowedOrigins;

    public WebConfig(
            LoginInterceptor loginInterceptor,
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.loginInterceptor = loginInterceptor;
        this.uploadDir = uploadDir;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/user/recover/code",
                        "/user/recover/account",
                        "/uploads/**",
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/*.ico",
                        "/*.svg",
                        "/*.png"
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
