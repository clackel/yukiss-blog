package moon.yukiss.utils;

import java.util.Map;

/**
 * ThreadLocal 工具类
 * 作用：像一个专属储物柜，每个请求线程进来，都有自己独立的小格子放东西。
 */
public class ThreadLocalUtil {
    // 提供 ThreadLocal 对象
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    // 放东西（存入用户信息）
    public static void set(Map<String, Object> map) {
        THREAD_LOCAL.set(map);
    }

    // 拿东西（获取用户信息）
    public static Map<String, Object> get() {
        return THREAD_LOCAL.get();
    }

    // 清空东西（防止内存泄漏！）
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}