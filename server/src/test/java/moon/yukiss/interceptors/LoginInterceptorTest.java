package moon.yukiss.interceptors;

import moon.yukiss.mapper.UserMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginInterceptorTest {
    private static final long ISSUED_AT = 1_790_000_000L;

    private JwtUtils jwtUtils;
    private UserMapper userMapper;
    private LoginInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        userMapper = mock(UserMapper.class);
        interceptor = new LoginInterceptor(jwtUtils, userMapper, new ObjectMapper());
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
        MockHttpServletRequest request = requestWithToken("GET", "/articles/12", "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        givenToken("valid-token", 7, true);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(7, ThreadLocalUtil.get().get("id"));
    }

    @Test
    void anonymousVisitorCanReadPublicUserProfile() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/12");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(ThreadLocalUtil.get());
    }

    @Test
    void privateMineEndpointRejectsMissingTokenWithJsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/articles/mine");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("登录状态无效"));
    }

    @Test
    void activeTokenIsAcceptedOnPrivateEndpoint() throws Exception {
        MockHttpServletRequest request = requestWithToken("GET", "/user/me", "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        givenToken("valid-token", 7, true);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(7, ThreadLocalUtil.get().get("id"));
        verify(userMapper).isTokenActive(7, ISSUED_AT);
    }

    @Test
    void revokedTokenIsRejectedOnPrivateEndpoint() throws Exception {
        // 改密码或注销后，数据库校验不通过
        MockHttpServletRequest request = requestWithToken("POST", "/articles", "revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        givenToken("revoked-token", 7, false);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertNull(ThreadLocalUtil.get());
    }

    @Test
    void revokedTokenFallsBackToAnonymousOnPublicRead() throws Exception {
        MockHttpServletRequest request = requestWithToken("GET", "/articles/12", "revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        givenToken("revoked-token", 7, false);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(ThreadLocalUtil.get());
    }

    @Test
    void legacyTokenWithoutIssuedAtIsRejected() throws Exception {
        MockHttpServletRequest request = requestWithToken("GET", "/user/me", "legacy-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.parseToken("legacy-token")).thenReturn(Map.of("id", 7));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        verify(userMapper, never()).isTokenActive(anyInt(), anyLong());
    }

    @Test
    void malformedTokenIsRejectedWithoutDatabaseLookup() throws Exception {
        MockHttpServletRequest request = requestWithToken("GET", "/user/me", "garbage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtils.parseToken("garbage")).thenThrow(new IllegalArgumentException("bad token"));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        verify(userMapper, never()).isTokenActive(anyInt(), anyLong());
    }

    private MockHttpServletRequest requestWithToken(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", token);
        return request;
    }

    private void givenToken(String token, int userId, boolean active) {
        when(jwtUtils.parseToken(token)).thenReturn(Map.of("id", userId, JwtUtils.ISSUED_AT_CLAIM, ISSUED_AT));
        when(userMapper.isTokenActive(userId, ISSUED_AT)).thenReturn(active);
    }
}
