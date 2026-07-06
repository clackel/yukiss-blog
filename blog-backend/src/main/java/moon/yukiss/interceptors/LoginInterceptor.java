package moon.yukiss.interceptors;

import moon.yukiss.utils.JwtUtils;
import moon.yukiss.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    public LoginInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行所有的 OPTIONS 请求（跨域预检请求）
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        // 1. 从请求头 (Header) 中取出名叫 "Authorization" 的手环 (Token)
        String token = request.getHeader("Authorization");
        boolean publicReadRequest = isPublicReadRequest(request);

        if (publicReadRequest && (token == null || token.isBlank())) {
            return true;
        }

        try {
            // 2. 验手环：调用 JwtUtils 解析 Token
            Map<String, Object> claims = jwtUtils.parseToken(token);

            // 3. 验明正身！把解析出来的用户信息放进“储物柜”(ThreadLocal)
            ThreadLocalUtil.set(claims);

            return true; // 验证通过，放行！
        } catch (Exception e) {
            // 如果解析报错（Token 失效、造假或没传），直接拦截并返回 401 状态码 (未授权)
            if (publicReadRequest) {
                return true;
            }
            response.setStatus(401);
            return false; // 不予放行
        }
    }

    private boolean isPublicReadRequest(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return "/articles".equals(uri)
                || uri.matches("/articles/\\d+")
                || "/comment/list".equals(uri);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求处理完（比如文章存完了），必须把储物柜清空，防止内存泄漏！
        ThreadLocalUtil.remove();
    }
}
