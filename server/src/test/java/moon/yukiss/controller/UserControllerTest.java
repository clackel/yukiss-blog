package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.BusinessException;
import moon.yukiss.dto.ChangePasswordRequest;
import moon.yukiss.dto.LoginRequest;
import moon.yukiss.entity.User;
import moon.yukiss.service.LoginAttemptService;
import moon.yukiss.service.UserService;
import moon.yukiss.utils.JwtUtils;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {
    private static final String IP = "203.0.113.7";

    private UserService userService;
    private JwtUtils jwtUtils;
    private LoginAttemptService loginAttemptService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtUtils = mock(JwtUtils.class);
        loginAttemptService = mock(LoginAttemptService.class);
        controller = new UserController(userService, jwtUtils, loginAttemptService);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void successfulLoginResetsFailuresAndReturnsToken() {
        when(userService.login("alice", "secret123")).thenReturn(user(7, "alice"));
        when(jwtUtils.genToken(any())).thenReturn("new-token");

        ApiResponse<Map<String, Object>> response = controller.login(login("alice", "secret123"), requestFrom(IP));

        assertEquals("new-token", response.getData().get("token"));
        verify(loginAttemptService).checkAllowed("alice", IP);
        verify(loginAttemptService).recordSuccess("alice", IP);
        verify(loginAttemptService, never()).recordFailure(anyString(), anyString());
    }

    @Test
    void failedLoginIsRecordedAndRethrown() {
        BusinessException failure = new BusinessException("用户名或密码不正确");
        when(userService.login("alice", "wrong-pass1")).thenThrow(failure);

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> controller.login(login("alice", "wrong-pass1"), requestFrom(IP))
        );

        assertSame(failure, thrown);
        verify(loginAttemptService).recordFailure("alice", IP);
        verify(loginAttemptService, never()).recordSuccess(anyString(), anyString());
    }

    @Test
    void lockedOutLoginNeverChecksPassword() {
        doThrow(BusinessException.tooManyRequests("登录失败次数过多"))
                .when(loginAttemptService).checkAllowed("alice", IP);

        assertThrows(BusinessException.class, () -> controller.login(login("alice", "secret123"), requestFrom(IP)));

        verify(userService, never()).login(anyString(), anyString());
        verify(loginAttemptService, never()).recordFailure(anyString(), anyString());
    }

    @Test
    void changePasswordReturnsFreshTokenForCurrentDevice() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(userService.getById(7)).thenReturn(user(7, "alice"));
        when(jwtUtils.genToken(any())).thenReturn("fresh-token");
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-pass1");
        request.setNewPassword("new-pass1");

        ApiResponse<Map<String, Object>> response = controller.changePassword(request);

        verify(userService).changePassword(7, "old-pass1", "new-pass1");
        assertEquals("fresh-token", response.getData().get("token"));
    }

    private LoginRequest login(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private MockHttpServletRequest requestFrom(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/login");
        request.setRemoteAddr(ip);
        return request;
    }

    private User user(int id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
