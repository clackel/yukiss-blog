package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.dto.PublicUserProfile;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.UserFollowMapper;
import moon.yukiss.service.NotificationService;
import moon.yukiss.service.UserFollowService;
import moon.yukiss.utils.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserFollowServiceImpl implements UserFollowService {
    private final UserFollowMapper userFollowMapper;
    private final NotificationService notificationService;

    public UserFollowServiceImpl(UserFollowMapper userFollowMapper, NotificationService notificationService) {
        this.userFollowMapper = userFollowMapper;
        this.notificationService = notificationService;
    }

    @Override
    public PublicUserProfile getPublicProfile(Integer userId) {
        if (userId == null || userId < 1) {
            throw BusinessException.notFound("用户不存在");
        }
        Integer currentUserId = CurrentUser.idOrNull();
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
        Integer currentUserId = CurrentUser.requireId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能关注自己");
        }
        getPublicProfile(userId);
        if (userFollowMapper.addFollow(currentUserId, userId) > 0) {
            notificationService.notify(userId, currentUserId, Notification.TYPE_FOLLOW, null, null);
        }
        return getPublicProfile(userId);
    }

    @Override
    @Transactional
    public PublicUserProfile unfollow(Integer userId) {
        Integer currentUserId = CurrentUser.requireId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能取消关注自己");
        }
        getPublicProfile(userId);
        if (userFollowMapper.deleteFollow(currentUserId, userId) > 0) {
            notificationService.retract(userId, currentUserId, Notification.TYPE_FOLLOW, null, null);
        }
        return getPublicProfile(userId);
    }
}
