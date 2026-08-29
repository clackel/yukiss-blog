package moon.yukiss.mapper;

import moon.yukiss.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);

    @Select("SELECT * FROM user WHERE id = #{id} AND is_deleted = 0")
    User findById(Integer id);

    @Select("SELECT * FROM user WHERE email = #{email} AND is_deleted = 0")
    User findByEmail(String email);

    @Insert("INSERT INTO user (username, password, nickname, email, email_verified, role, create_time, password_updated_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{email}, #{emailVerified}, 'USER', NOW(), NOW())")
    void insert(User user);

    @Update("UPDATE user SET avatar = #{avatarUrl} WHERE id = #{id} AND is_deleted = 0")
    void updateAvatar(@Param("avatarUrl") String avatarUrl, @Param("id") Integer id);

    @Update("UPDATE user SET nickname = #{nickname}, bio = #{bio}, gender = #{gender}, birthday = #{birthday}, location = #{location}, website = #{website} WHERE id = #{id} AND is_deleted = 0")
    void updateProfile(User user);

    @Update("UPDATE user SET password = #{password}, password_updated_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    void updatePassword(@Param("id") Integer id, @Param("password") String password);

    @Update("UPDATE user SET email = #{email}, email_verified = 1 WHERE id = #{id} AND is_deleted = 0")
    void bindEmail(@Param("id") Integer id, @Param("email") String email);

    @Update("UPDATE user SET last_login_time = NOW() WHERE id = #{id}")
    void updateLastLoginTime(Integer id);

    @Update("UPDATE user SET is_deleted = 1, deleted_time = NOW(), email = NULL WHERE id = #{id}")
    void deleteById(Integer id);
}
