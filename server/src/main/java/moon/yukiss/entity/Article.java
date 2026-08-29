package moon.yukiss.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Article {
    private Integer id;
    private Integer authorId;
    private String title;
    private String content;
    private Date createTime;
    private Date updateTime;
    private Date lastCommentTime;
    private String authorNickname;
    private String authorAvatar;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean likedByMe;
}
