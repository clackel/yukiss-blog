package moon.yukiss.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import moon.yukiss.common.ApiResponse;
import moon.yukiss.dto.BindEmailRequest;
import moon.yukiss.dto.ChangePasswordRequest;
import moon.yukiss.dto.DeleteAccountRequest;
import moon.yukiss.dto.LoginRequest;
import moon.yukiss.dto.RecoverAccountRequest;
import moon.yukiss.dto.RegisterRequest;
import moon.yukiss.dto.SendEmailCodeRequest;
import moon.yukiss.dto.UpdateProfileRequest;
import moon.yukiss.common.BusinessException;
import moon.yukiss.entity.User;
import moon.yukiss.service.LoginAttemptService;
import moon.yukiss.service.UserService;
import moon.yukiss.utils.JwtUtils;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final LoginAttemptService loginAttemptService;

    public UserController(UserService userService, JwtUtils jwtUtils, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = httpRequest.getRemoteAddr();
        loginAttemptService.checkAllowed(request.getUsername(), clientIp);
        User user;
        try {
            user = userService.login(request.getUsername(), request.getPassword());
        } catch (BusinessException ex) {
            loginAttemptService.recordFailure(request.getUsername(), clientIp);
            throw ex;
        }
        loginAttemptService.recordSuccess(request.getUsername(), clientIp);

        Map<String, Object> result = new HashMap<>();
        result.put("token", issueToken(user));
        result.put("user", user);
        return ApiResponse.ok(result);
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<User> me() {
        return ApiResponse.ok(userService.getById(currentUserId()));
    }

    @PutMapping("/profile")
    public ApiResponse<User> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(currentUserId(), request));
    }

    /**
     * 修改密码会使此前签发的所有 token 失效，这里返回新 token 让当前设备保持登录。
     */
    @PostMapping("/change-password")
    public ApiResponse<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Integer userId = currentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.ok("密码已更新，其他设备需要重新登录", Map.of("token", issueToken(userService.getById(userId))));
    }

    @PostMapping("/email/code")
    public ApiResponse<Map<String, Object>> sendBindEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        String code = userService.sendEmailBindCode(currentUserId(), request.getEmail());
        return codeResponse("验证码已发送，请在 10 分钟内完成绑定", code);
    }

    @PostMapping("/email/bind")
    public ApiResponse<User> bindEmail(@Valid @RequestBody BindEmailRequest request) {
        return ApiResponse.ok(userService.bindEmail(currentUserId(), request.getEmail(), request.getCode()));
    }

    @PostMapping("/recover/code")
    public ApiResponse<Map<String, Object>> sendRecoverCode(@Valid @RequestBody SendEmailCodeRequest request) {
        String code = userService.sendRecoverCode(request.getEmail());
        return codeResponse("验证码已发送，请检查邮箱", code);
    }

    @PostMapping("/recover/account")
    public ApiResponse<Map<String, String>> recoverAccount(@Valid @RequestBody RecoverAccountRequest request) {
        String username = userService.recoverAccount(request.getEmail(), request.getCode());
        return ApiResponse.ok(Map.of("username", username));
    }

    @PostMapping("/updateAvatar")
    public ApiResponse<User> updateAvatar(String avatarUrl) {
        return ApiResponse.ok(userService.updateAvatar(avatarUrl, currentUserId()));
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(currentUserId(), request.getPassword());
        return ApiResponse.ok("账号已注销，登录状态已失效");
    }

    private String issueToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        return jwtUtils.genToken(claims);
    }

    private Integer currentUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }

    private ApiResponse<Map<String, Object>> codeResponse(String message, String code) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        if (code != null) {
            data.put("devCode", code);
        }
        return ApiResponse.ok(data);
    }
}
