package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.dto.RegisterRequest;
import moon.yukiss.dto.UpdateProfileRequest;
import moon.yukiss.entity.User;
import moon.yukiss.mapper.UserMapper;
import moon.yukiss.service.EmailCodeService;
import moon.yukiss.service.UserService;
import moon.yukiss.utils.PasswordHasher;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final EmailCodeService emailCodeService;

    public UserServiceImpl(UserMapper userMapper, PasswordHasher passwordHasher, EmailCodeService emailCodeService) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.emailCodeService = emailCodeService;
    }

    @Override
    public User login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        User user = userMapper.findByUsername(normalizedUsername);
        if (user == null) {
            throw new BusinessException("用户名或密码不正确");
        }

        boolean matched = passwordHasher.matches(password, user.getPassword());
        if (!matched && passwordHasher.needsUpgrade(user.getPassword())) {
            matched = user.getPassword().equals(password);
            if (matched) {
                userMapper.updatePassword(user.getId(), passwordHasher.hash(password));
            }
        }

        if (!matched) {
            throw new BusinessException("用户名或密码不正确");
        }

        userMapper.updateLastLoginTime(user.getId());
        user.setPassword(null);
        return userMapper.findById(user.getId());
    }

    @Override
    public User register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordHasher.hash(request.getPassword()));
        user.setNickname(normalizeText(request.getNickname(), 30, username));
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = emailCodeService.normalizeEmail(request.getEmail());
            if (userMapper.findByEmail(email) != null) {
                throw new BusinessException("该邮箱已被绑定");
            }
            user.setEmail(email);
            user.setEmailVerified(false);
        } else {
            user.setEmailVerified(false);
        }
        userMapper.insert(user);
        return userMapper.findByUsername(username);
    }

    @Override
    public User getById(Integer id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在或已注销");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User updateProfile(Integer id, UpdateProfileRequest request) {
        User user = getById(id);
        user.setNickname(normalizeText(request.getNickname(), 30, user.getUsername()));
        user.setBio(normalizeText(request.getBio(), 300, null));
        user.setGender(normalizeText(request.getGender(), 20, null));
        user.setBirthday(request.getBirthday());
        user.setLocation(normalizeText(request.getLocation(), 80, null));
        user.setWebsite(normalizeText(request.getWebsite(), 180, null));
        userMapper.updateProfile(user);
        return getById(id);
    }

    @Override
    public void changePassword(Integer id, String oldPassword, String newPassword) {
        User user = userMapper.findById(id);
        if (user == null || !passwordHasher.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        userMapper.updatePassword(id, passwordHasher.hash(newPassword));
    }

    @Override
    public User bindEmail(Integer id, String email, String code) {
        String normalizedEmail = emailCodeService.normalizeEmail(email);
        User existing = userMapper.findByEmail(normalizedEmail);
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }
        emailCodeService.verify(normalizedEmail, "bind", code);
        userMapper.bindEmail(id, normalizedEmail);
        return getById(id);
    }

    @Override
    public String sendEmailBindCode(Integer id, String email) {
        getById(id);
        String normalizedEmail = emailCodeService.normalizeEmail(email);
        User existing = userMapper.findByEmail(normalizedEmail);
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }
        return emailCodeService.createCode(normalizedEmail, "bind");
    }

    @Override
    public String sendRecoverCode(String email) {
        String normalizedEmail = emailCodeService.normalizeEmail(email);
        if (userMapper.findByEmail(normalizedEmail) == null) {
            throw new BusinessException("没有找到绑定该邮箱的账号");
        }
        return emailCodeService.createCode(normalizedEmail, "recover");
    }

    @Override
    public String recoverAccount(String email, String code) {
        String normalizedEmail = emailCodeService.normalizeEmail(email);
        emailCodeService.verify(normalizedEmail, "recover", code);
        User user = userMapper.findByEmail(normalizedEmail);
        if (user == null) {
            throw new BusinessException("没有找到绑定该邮箱的账号");
        }
        return user.getUsername();
    }

    @Override
    public User updateAvatar(String avatarUrl, Integer id) {
        getById(id);
        userMapper.updateAvatar(avatarUrl, id);
        return getById(id);
    }

    @Override
    public void deleteAccount(Integer id, String password) {
        User user = userMapper.findById(id);
        if (user == null || !passwordHasher.matches(password, user.getPassword())) {
            throw new BusinessException("密码确认失败，无法注销账号");
        }
        userMapper.deleteById(id);
    }

    private String normalizeUsername(String username) {
        if (username == null || !username.matches("^[A-Za-z0-9_]{3,20}$")) {
            throw new BusinessException("用户名需要 3-20 位，只能包含字母、数字和下划线");
        }
        return username.trim();
    }

    private String normalizeText(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException("输入内容不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }
}
