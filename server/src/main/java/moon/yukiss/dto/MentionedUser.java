package moon.yukiss.dto;

import lombok.Data;

/** 评论中被 @ 提及的用户，供前端把 @username 渲染成主页链接 */
@Data
public class MentionedUser {
    private Integer id;
    private String username;
    private String nickname;
}
