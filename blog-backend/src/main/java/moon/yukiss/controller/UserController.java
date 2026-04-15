package moon.yukiss.controller;

import moon.yukiss.entity.User;
import moon.yukiss.service.UserService;
import moon.yukiss.utils.JwtUtils;
import moon.yukiss.utils.ThreadLocalUtil;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 处理登录请求 (通常用 POST，把敏感信息放在请求体里)
    @PostMapping("/login")
    public Object login(@RequestBody User loginUser) {
        User user = userService.login(loginUser.getUsername(), loginUser.getPassword());
        if (user != null) {
            // 登录成功，分配Token
            // 把用户的 id 和 username 塞进Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", user.getId());
            claims.put("username", user.getUsername());
            String token = JwtUtils.genToken(claims);

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);

            user.setPassword("");
            result.put("user", user);

            return result; // 返回Token
        }
        return "登录失败：用户名或密码错误";
    }

    // 处理注册请求
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        try {
            userService.register(user);
            return "注册成功！";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // 更新头像请求
    @PostMapping("/updateAvatar")
    public String updateAvatar(String avatarUrl) {
        // 从储物柜里拿出当前登录的用户信息
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        // 更新数据库
        userService.updateAvatar(avatarUrl, userId);
        return "头像更新成功！";
    }

    @DeleteMapping("/delete")
    public String deleteAccount(Integer id) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        userService.deleteAccount(userId);

        return "账号已注销！";
    }
}