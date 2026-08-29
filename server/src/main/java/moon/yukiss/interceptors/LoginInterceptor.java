package moon.yukiss.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moon.yukiss.common.ApiResponse;
import moon.yukiss.utils.JwtUtils;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (isPublicRead(request)) {
            if (token != null && !token.isBlank()) {
                try {
                    ThreadLocalUtil.set(jwtUtils.parseToken(token));
                } catch (Exception ignored) {
                    ThreadLocalUtil.remove();
                }
            }
            return true;
        }

        if (token == null || token.isBlank()) {
            writeUnauthorized(response);
            return false;
        }

        try {
            Map<String, Object> claims = jwtUtils.parseToken(token);
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception ex) {
            ThreadLocalUtil.remove();
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        ThreadLocalUtil.remove();
    }

    private boolean isPublicRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.equals("/articles") || uri.equals("/articles/page") || uri.equals("/comment/list")) {
            return true;
        }
        if (uri.matches("^/users/\\d+$")) {
            return true;
        }
        return uri.startsWith("/articles/") && !uri.equals("/articles/mine");
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail("登录状态无效，请重新登录"));
    }
}
