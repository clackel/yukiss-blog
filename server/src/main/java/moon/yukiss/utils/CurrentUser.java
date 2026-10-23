package moon.yukiss.utils;

import moon.yukiss.common.BusinessException;

import java.util.Map;

/** 从 ThreadLocalUtil 中读取当前登录用户（由 LoginInterceptor 写入）。 */
public final class CurrentUser {
    private CurrentUser() {
    }

    /** 未登录（或公开接口匿名访问）时返回 null */
    public static Integer idOrNull() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            return null;
        }
        Object id = claims.get("id");
        return id instanceof Number number ? number.intValue() : null;
    }

    public static Integer requireId() {
        Integer id = idOrNull();
        if (id == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return id;
    }
}
