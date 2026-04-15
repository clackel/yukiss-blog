package moon.yukiss.mapper;

import moon.yukiss.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
public interface UserMapper {
    // 根据用户名查询用户（登录用）
    @Select("SELECT * FROM user WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);

    // 注册新用户
    @Insert("INSERT INTO user (username, password, nickname, role, create_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, 'USER', NOW())")
    void insert(User user);

    @org.apache.ibatis.annotations.Update("UPDATE user SET avatar = #{avatarUrl} WHERE id = #{id}")
    void updateAvatar(String avatarUrl, Integer id);

    // 注销账户
    @Update("UPDATE user SET is_deleted = 1 WHERE id = #{id}")
    void deleteById(Integer id);
}
