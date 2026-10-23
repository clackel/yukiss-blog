package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.NotificationMapper;
import moon.yukiss.service.impl.NotificationServiceImpl;
import moon.yukiss.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {
    private NotificationMapper notificationMapper;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        notificationService = new NotificationServiceImpl(notificationMapper);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void ignoresActionsOnYourOwnContent() {
        notificationService.notify(7, 7, Notification.TYPE_ARTICLE_LIKE, 1, null);
        notificationService.retract(7, 7, Notification.TYPE_ARTICLE_LIKE, 1, null);
        notificationService.notify(null, 7, Notification.TYPE_COMMENT, 1, 2);

        verify(notificationMapper, never()).insert(any(), any(), any(), any(), any());
        verify(notificationMapper, never()).deleteMatching(any(), any(), any(), any(), any());
    }

    @Test
    void notifiesOtherUsers() {
        notificationService.notify(3, 7, Notification.TYPE_COMMENT, 1, 2);

        verify(notificationMapper).insert(3, 7, Notification.TYPE_COMMENT, 1, 2);
    }

    @Test
    void skipsEmptyCommentCleanup() {
        notificationService.deleteByCommentIds(List.of());

        verify(notificationMapper, never()).deleteByCommentIds(anyList());
    }

    @Test
    void pageOnlyReadsCurrentUsersNotifications() {
        ThreadLocalUtil.set(Map.of("id", 7));
        Notification item = new Notification();
        when(notificationMapper.count(7)).thenReturn(21L);
        when(notificationMapper.page(7, 20, 20)).thenReturn(List.of(item));

        PageResult<Notification> result = notificationService.page(2, 20);

        assertEquals(List.of(item), result.getItems());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void markingSomeoneElsesNotificationLooksLikeNotFound() {
        ThreadLocalUtil.set(Map.of("id", 7));
        when(notificationMapper.markRead(99, 7)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> notificationService.markRead(99));

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void requiresLogin() {
        BusinessException exception = assertThrows(BusinessException.class, () -> notificationService.unreadCount());

        assertEquals(401, exception.getStatus().value());
        verify(notificationMapper, never()).countUnread(anyInt());
    }
}
