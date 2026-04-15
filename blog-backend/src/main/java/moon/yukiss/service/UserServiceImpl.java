package moon.yukiss.service;

import moon.yukiss.entity.User;
import moon.yukiss.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    // 注册账户
    @Override
    public void register(User user) {
        // 先检查用户名是否已存在
        User existUser = userMapper.findByUsername(user.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在！");
        }
        // 如果没有设置昵称，默认使用用户名
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            user.setNickname(user.getUsername());
        }
        userMapper.insert(user);
    }

    // login
    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        // 简单比对密码 (实际项目中必须使用加密，如 BCrypt！)
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null; // 登录失败
    }

    // 注销账户
    @Override
    public void deleteAccount(Integer id){
        userMapper.deleteById(id);
    }

    // 更新头像
    @Override
    public void updateAvatar(String avatarUrl, Integer id) {
        userMapper.updateAvatar(avatarUrl, id);
    }


}