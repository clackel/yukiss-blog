package moon.yukiss.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArticleComment {
    private Integer id;
    private Integer articleId;
    private Integer parentId;
    private Integer userId;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    /** 编辑过的评论才有值 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
    private String nickname;
    private String avatar;
    /** 评论者已注销时为 true，此时昵称显示为“已注销用户”且不返回头像 */
    private Boolean userDeleted;
    private String parentNickname;
    private Integer likeCount;
    private Boolean likedByMe;
    /** 分页接口中一级评论携带的全部回复，回复本身为空列表 */
    private List<ArticleComment> replies = new ArrayList<>();
}
