package moon.yukiss.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moon.yukiss.common.ApiResponse;
import moon.yukiss.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(JwtUtils jwtUtils, UserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
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
            // 公开读取：token 无效或已失效时按匿名访问处理
            Map<String, Object> claims = resolveActiveClaims(token);
            if (claims != null) {
                ThreadLocalUtil.set(claims);
            }
            return true;
        }

        Map<String, Object> claims = resolveActiveClaims(token);
        if (claims == null) {
            writeUnauthorized(response);
            return false;
        }
        ThreadLocalUtil.set(claims);
        return true;
    }

    /**
     * 校验签名与过期时间，并确认账号未注销、token 签发后未修改过密码。
     * 任一条件不满足返回 null。
     */
    private Map<String, Object> resolveActiveClaims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Map<String, Object> claims;
        try {
            claims = jwtUtils.parseToken(token);
        } catch (Exception ex) {
            return null;
        }
        if (!(claims.get("id") instanceof Number id)
                || !(claims.get(JwtUtils.ISSUED_AT_CLAIM) instanceof Number issuedAt)) {
            return null;
        }
        return userMapper.isTokenActive(id.intValue(), issuedAt.longValue()) ? claims : null;
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
