package moon.yukiss.interceptors;

import moon.yukiss.utils.JwtUtils;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginInterceptorTest {
    private JwtUtils jwtUtils;
    private LoginInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        interceptor = new LoginInterceptor(jwtUtils, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void anonymousVisitorCanReadPublicArticleList() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/articles/page");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(ThreadLocalUtil.get());
    }

    @Test
    void validOptionalTokenProvidesCurrentUserOnPublicRead() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/articles/12");
        request.addHeader("Authorization", "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.parseToken("valid-token")).thenReturn(Map.of("id", 7));

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(7, ThreadLocalUtil.get().get("id"));
    }

    @Test
    void privateMineEndpointRejectsMissingTokenWithJsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/articles/mine");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertEquals(false, allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("登录状态无效"));
    }
}
