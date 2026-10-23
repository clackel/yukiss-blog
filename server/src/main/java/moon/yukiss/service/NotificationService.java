package moon.yukiss.service;

import moon.yukiss.common.PageResult;
import moon.yukiss.entity.Notification;

import java.util.List;

public interface NotificationService {
    /** 给 recipient 发送通知；recipient 为空或就是 actor 本人时忽略 */
    void notify(Integer recipientId, Integer actorId, String type, Integer articleId, Integer commentId);

    /** 撤回之前发出的同一条通知（取消点赞、取消关注） */
    void retract(Integer recipientId, Integer actorId, String type, Integer articleId, Integer commentId);

    void deleteByCommentIds(List<Integer> commentIds);

    PageResult<Notification> page(Integer page, Integer pageSize);

    long unreadCount();

    void markRead(Integer id);

    void markAllRead();
}
