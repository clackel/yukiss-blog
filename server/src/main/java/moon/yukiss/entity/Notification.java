package moon.yukiss.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Notification {
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_REPLY = "reply";
    public static final String TYPE_ARTICLE_LIKE = "article_like";
    public static final String TYPE_COMMENT_LIKE = "comment_like";
    public static final String TYPE_FOLLOW = "follow";

    private Integer id;
    private String type;
    private Integer actorId;
    private String actorNickname;
    private String actorAvatar;
    private Boolean actorDeleted;
    private Integer articleId;
    private String articleTitle;
    private Integer commentId;
    /** 评论 / 回复 / 评论点赞时为对应评论内容 */
    private String commentContent;
    private Boolean read;
    private Date createTime;
}
