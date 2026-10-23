package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.dto.PublicUserProfile;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.UserFollowMapper;
import moon.yukiss.service.impl.UserFollowServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserFollowServiceImplTest {
    private UserFollowMapper userFollowMapper;
    private NotificationService notificationService;
    private UserFollowServiceImpl userFollowService;

    @BeforeEach
    void setUp() {
        userFollowMapper = mock(UserFollowMapper.class);
        notificationService = mock(NotificationService.class);
        userFollowService = new UserFollowServiceImpl(userFollowMapper, notificationService);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void publicProfileMarksCurrentUsersOwnPage() {
        ThreadLocalUtil.set(Map.of("id", 7));
        PublicUserProfile profile = profile(7);
        when(userFollowMapper.findPublicProfile(7, 7)).thenReturn(profile);

        PublicUserProfile result = userFollowService.getPublicProfile(7);

        assertTrue(result.getOwnProfile());
    }

    @Test
    void followCreatesRelationAndReturnsUpdatedProfile() {
        ThreadLocalUtil.set(Map.of("id", 7));
        PublicUserProfile before = profile(9);
        PublicUserProfile after = profile(9);
        after.setFollowedByMe(true);
        after.setFollowerCount(1);
        when(userFollowMapper.findPublicProfile(9, 7)).thenReturn(before, after);
        when(userFollowMapper.addFollow(7, 9)).thenReturn(1);

        PublicUserProfile result = userFollowService.follow(9);

        verify(userFollowMapper).addFollow(7, 9);
        verify(notificationService).notify(9, 7, Notification.TYPE_FOLLOW, null, null);
        assertTrue(result.getFollowedByMe());
        assertEquals(1, result.getFollowerCount());
    }

    @Test
    void followingAgainDoesNotSendAnotherNotification() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(userFollowMapper.findPublicProfile(9, 7)).thenReturn(profile(9));
        when(userFollowMapper.addFollow(7, 9)).thenReturn(0);

        userFollowService.follow(9);

        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    void unfollowRetractsFollowNotification() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(userFollowMapper.findPublicProfile(9, 7)).thenReturn(profile(9));
        when(userFollowMapper.deleteFollow(7, 9)).thenReturn(1);

        userFollowService.unfollow(9);

        verify(notificationService).retract(9, 7, Notification.TYPE_FOLLOW, null, null);
    }

    @Test
    void followRejectsOwnProfile() {
        ThreadLocalUtil.set(Map.of("id", 7));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userFollowService.follow(7)
        );

        assertEquals("不能关注自己", exception.getMessage());
    }

    private PublicUserProfile profile(int id) {
        PublicUserProfile profile = new PublicUserProfile();
        profile.setId(id);
        profile.setFollowerCount(0);
        profile.setFollowingCount(0);
        profile.setFollowedByMe(false);
        return profile;
    }
}
