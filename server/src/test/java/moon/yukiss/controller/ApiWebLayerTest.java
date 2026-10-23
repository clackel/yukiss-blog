package moon.yukiss.controller;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.GlobalExceptionHandler;
import moon.yukiss.common.PageResult;
import moon.yukiss.interceptors.LoginInterceptor;
import moon.yukiss.mapper.UserMapper;
import moon.yukiss.service.ArticleCommentService;
import moon.yukiss.service.ArticleService;
import moon.yukiss.service.NotificationService;
import moon.yukiss.utils.JwtUtils;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web 层测试：真实的拦截器、参数校验和全局异常处理，service 使用 mock。
 * 覆盖 service 单元测试测不到的部分——鉴权、请求参数校验、异常到 HTTP 状态码的映射。
 */
class ApiWebLayerTest {
    private static final String TOKEN = "valid-token";
    private static final long ISSUED_AT = 1_790_000_000L;

    private ArticleService articleService;
    private ArticleCommentService commentService;
    private NotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        articleService = mock(ArticleService.class);
        commentService = mock(ArticleCommentService.class);
        notificationService = mock(NotificationService.class);
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(jwtUtils.parseToken(TOKEN)).thenReturn(Map.of("id", 7, JwtUtils.ISSUED_AT_CLAIM, ISSUED_AT));
        when(userMapper.isTokenActive(7, ISSUED_AT)).thenReturn(true);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ArticleController(articleService),
                        new ArticleCommentController(commentService),
                        new NotificationController(notificationService)
                )
                .addInterceptors(new LoginInterceptor(jwtUtils, userMapper, new ObjectMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void anonymousVisitorCanBrowseArticlesWithDefaults() throws Exception {
        when(articleService.page(1, 10, null, "published", null)).thenReturn(PageResult.of(List.of(), 0, 1, 10));

        mockMvc.perform(get("/articles/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void popularTagsArePublic() throws Exception {
        mockMvc.perform(get("/articles/tags")).andExpect(status().isOk());
    }

    @Test
    void writingWithoutTokenIsRejectedBeforeReachingService() throws Exception {
        mockMvc.perform(post("/articles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "标题", "content": "正文"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("登录状态无效，请重新登录"));
        verify(articleService, never()).add(any());
    }

    @Test
    void blankTitleFailsValidation() throws Exception {
        mockMvc.perform(post("/articles")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  ", "content": "正文"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写文章标题"));
        verify(articleService, never()).add(any());
    }

    @Test
    void tooManyTagsFailsValidation() throws Exception {
        mockMvc.perform(post("/articles")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "标题", "content": "正文", "tags": ["a", "b", "c", "d", "e", "f"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("每篇文章最多 5 个标签"));
    }

    @Test
    void businessExceptionStatusIsPreserved() throws Exception {
        when(articleService.update(eq(5), any())).thenThrow(BusinessException.forbidden("只能修改或删除自己的文章"));

        mockMvc.perform(put("/articles/5")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "标题", "content": "正文"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能修改或删除自己的文章"));
    }

    @Test
    void malformedQueryParameterIsBadRequest() throws Exception {
        mockMvc.perform(get("/comment/list").param("articleId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求参数不合法"));
    }

    @Test
    void deletingCommentReturnsDeletedCount() throws Exception {
        when(commentService.deleteComment(9)).thenReturn(3);

        mockMvc.perform(delete("/comment/9").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("评论已删除"))
                .andExpect(jsonPath("$.data.deletedCount").value(3));
    }

    @Test
    void editingCommentRequiresContent() throws Exception {
        mockMvc.perform(put("/comment/9")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写评论内容"));
    }

    @Test
    void notificationsRequireLogin() throws Exception {
        mockMvc.perform(get("/notifications/unread-count")).andExpect(status().isUnauthorized());

        when(notificationService.unreadCount()).thenReturn(4L);
        mockMvc.perform(get("/notifications/unread-count").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(4));
    }

    @Test
    void tooManyRequestsIsMappedTo429() throws Exception {
        when(notificationService.unreadCount()).thenThrow(BusinessException.tooManyRequests("请求过于频繁"));

        mockMvc.perform(get("/notifications/unread-count").header("Authorization", TOKEN))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("请求过于频繁"));
    }
}
