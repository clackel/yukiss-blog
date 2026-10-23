package moon.yukiss.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Data
public class Article {
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_DRAFT = "draft";

    private Integer id;
    private Integer authorId;
    private String title;
    private String content;
    /** published 或 draft，草稿仅作者本人可见 */
    private String status;
    private Date createTime;
    private Date updateTime;
    private Date lastCommentTime;
    private String authorNickname;
    private String authorAvatar;
    /** 作者已注销时为 true，此时昵称显示为“已注销用户”且不返回头像 */
    private Boolean authorDeleted;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean likedByMe;

    /** 查询时由 GROUP_CONCAT 拼接的标签，逗号分隔；标签本身不允许包含逗号 */
    @JsonIgnore
    private String tagNames;

    @JsonProperty("tags")
    public List<String> getTags() {
        if (tagNames == null || tagNames.isBlank()) {
            return List.of();
        }
        return Arrays.asList(tagNames.split(","));
    }

    @JsonIgnore
    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }
}
