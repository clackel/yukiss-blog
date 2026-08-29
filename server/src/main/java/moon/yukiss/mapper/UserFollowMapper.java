package moon.yukiss.mapper;

import moon.yukiss.dto.PublicUserProfile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFollowMapper {
    PublicUserProfile findPublicProfile(
            @Param("userId") Integer userId,
            @Param("currentUserId") Integer currentUserId
    );

    @Insert("""
            INSERT IGNORE INTO user_follow(follower_id, following_id, create_time)
            VALUES(#{followerId}, #{followingId}, NOW())
            """)
    int addFollow(
            @Param("followerId") Integer followerId,
            @Param("followingId") Integer followingId
    );

    @Delete("""
            DELETE FROM user_follow
            WHERE follower_id = #{followerId} AND following_id = #{followingId}
            """)
    int deleteFollow(
            @Param("followerId") Integer followerId,
            @Param("followingId") Integer followingId
    );

    @Delete("DELETE FROM user_follow WHERE follower_id = #{userId} OR following_id = #{userId}")
    int deleteAllByUserId(@Param("userId") Integer userId);
}
