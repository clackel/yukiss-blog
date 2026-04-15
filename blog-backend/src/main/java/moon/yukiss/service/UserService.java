package moon.yukiss.service;

import moon.yukiss.entity.User;

public interface UserService {
    // 登录：检查用户名和密码
    User login(String username, String password);

    // 注册：验证用户名是否重复等
    void register(User user);

    void updateAvatar(String avatarUrl, Integer id);

    // 注销
    void deleteAccount(Integer id);
}