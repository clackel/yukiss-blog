package moon.yukiss.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PublicUserProfile {
    private Integer id;
    private String nickname;
    private String avatar;
    private String bio;
    private String location;
    private String website;
    private Date createTime;
    private Integer followerCount;
    private Integer followingCount;
    private Boolean followedByMe;
    private Boolean ownProfile;
}
