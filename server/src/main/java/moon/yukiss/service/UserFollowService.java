package moon.yukiss.service;

import moon.yukiss.dto.PublicUserProfile;

public interface UserFollowService {
    PublicUserProfile getPublicProfile(Integer userId);

    PublicUserProfile follow(Integer userId);

    PublicUserProfile unfollow(Integer userId);
}
