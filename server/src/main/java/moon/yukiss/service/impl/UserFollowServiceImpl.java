package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.dto.PublicUserProfile;
import moon.yukiss.mapper.UserFollowMapper;
import moon.yukiss.service.UserFollowService;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserFollowServiceImpl implements UserFollowService {
    private final UserFollowMapper userFollowMapper;

    public UserFollowServiceImpl(UserFollowMapper userFollowMapper) {
        this.userFollowMapper = userFollowMapper;
    }

    @Override
    public PublicUserProfile getPublicProfile(Integer userId) {
        if (userId == null || userId < 1) {
            throw BusinessException.notFound("用户不存在");
        }
        Integer currentUserId = currentUserIdOrNull();
        PublicUserProfile profile = userFollowMapper.findPublicProfile(userId, currentUserId);
        if (profile == null) {
            throw BusinessException.notFound("用户不存在或已注销");
        }
        profile.setOwnProfile(userId.equals(currentUserId));
        return profile;
    }

    @Override
    @Transactional
    public PublicUserProfile follow(Integer userId) {
        Integer currentUserId = requireCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能关注自己");
        }
        getPublicProfile(userId);
        userFollowMapper.addFollow(currentUserId, userId);
        return getPublicProfile(userId);
    }

    @Override
    @Transactional
    public PublicUserProfile unfollow(Integer userId) {
        Integer currentUserId = requireCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能取消关注自己");
        }
        getPublicProfile(userId);
        userFollowMapper.deleteFollow(currentUserId, userId);
        return getPublicProfile(userId);
    }

    private Integer currentUserIdOrNull() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            return null;
        }
        Object id = claims.get("id");
        return id instanceof Number ? ((Number) id).intValue() : null;
    }

    private Integer requireCurrentUserId() {
        Integer userId = currentUserIdOrNull();
        if (userId == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return userId;
    }
}
