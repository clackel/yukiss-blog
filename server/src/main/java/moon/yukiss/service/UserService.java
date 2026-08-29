package moon.yukiss.service;

import moon.yukiss.dto.RegisterRequest;
import moon.yukiss.dto.UpdateProfileRequest;
import moon.yukiss.entity.User;

public interface UserService {
    User login(String username, String password);

    User register(RegisterRequest request);

    User getById(Integer id);

    User updateProfile(Integer id, UpdateProfileRequest request);

    void changePassword(Integer id, String oldPassword, String newPassword);

    User bindEmail(Integer id, String email, String code);

    String sendEmailBindCode(Integer id, String email);

    String sendRecoverCode(String email);

    String recoverAccount(String email, String code);

    User updateAvatar(String avatarUrl, Integer id);

    void deleteAccount(Integer id, String password);
}
