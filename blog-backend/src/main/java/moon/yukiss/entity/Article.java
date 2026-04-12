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
    private String authorNickname;
    private String authorAvatar;
}