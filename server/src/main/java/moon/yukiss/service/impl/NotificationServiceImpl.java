package moon.yukiss.service.impl;

import moon.yukiss.common.BusinessException;
import moon.yukiss.common.PageResult;
import moon.yukiss.entity.Notification;
import moon.yukiss.mapper.NotificationMapper;
import moon.yukiss.service.NotificationService;
import moon.yukiss.utils.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void notify(Integer recipientId, Integer actorId, String type, Integer articleId, Integer commentId) {
        if (recipientId == null || recipientId.equals(actorId)) {
            return;
        }
        notificationMapper.insert(recipientId, actorId, type, articleId, commentId);
    }

    @Override
    public void retract(Integer recipientId, Integer actorId, String type, Integer articleId, Integer commentId) {
        if (recipientId == null || recipientId.equals(actorId)) {
            return;
        }
        notificationMapper.deleteMatching(recipientId, actorId, type, articleId, commentId);
    }

    @Override
    public void deleteByCommentIds(List<Integer> commentIds) {
        if (commentIds != null && !commentIds.isEmpty()) {
            notificationMapper.deleteByCommentIds(commentIds);
        }
    }

    @Override
    public PageResult<Notification> page(Integer page, Integer pageSize) {
        Integer userId = CurrentUser.requireId();
        int normalizedPage = page == null ? 1 : page;
        int normalizedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalizedPage < 1) {
            throw new BusinessException("页码必须大于等于 1");
        }
        if (normalizedPageSize < 1 || normalizedPageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量必须在 1 到 50 之间");
        }
        long total = notificationMapper.count(userId);
        List<Notification> items = total == 0
                ? List.of()
                : notificationMapper.page(userId, (normalizedPage - 1) * normalizedPageSize, normalizedPageSize);
        return PageResult.of(items, total, normalizedPage, normalizedPageSize);
    }

    @Override
    public long unreadCount() {
        return notificationMapper.countUnread(CurrentUser.requireId());
    }

    @Override
    public void markRead(Integer id) {
        Integer userId = CurrentUser.requireId();
        if (id == null || notificationMapper.markRead(id, userId) == 0) {
            throw BusinessException.notFound("通知不存在");
        }
    }

    @Override
    public void markAllRead() {
        notificationMapper.markAllRead(CurrentUser.requireId());
    }
}
